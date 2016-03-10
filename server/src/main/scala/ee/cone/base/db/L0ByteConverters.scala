
package ee.cone.base.db

import java.nio.charset.StandardCharsets.UTF_8

import ee.cone.base.util.{LongFits, UInt, Bytes, Never}
import ee.cone.base.db.Types._

// converters //////////////////////////////////////////////////////////////////

class RawFactConverterImpl extends RawFactConverter {
  def head = 0L
  def key(objId: ObjId, attrId: RawAttr[_]): RawKey =
    key(objId, attrId.labelId, attrId.propId, hasObjId=true, hasAttrId=true)
  def keyWithoutAttrId(objId: ObjId): RawKey =
    key(objId, 0, 0, hasObjId=true, hasAttrId=false)
  def keyHeadOnly: RawKey =
    key(0, 0, 0, hasObjId=false, hasAttrId=false)
  private def key(objId: Long, labelId: Long, propId: Long, hasObjId: Boolean, hasAttrId: Boolean): RawKey = {
    val exHead = CompactBytes.toWrite(head).at(0)
    exHead.write(head, if(!hasObjId) exHead.alloc(0) else {
      val exObjId = CompactBytes.toWrite(objId).after(exHead)
      exObjId.write(objId, if(!hasAttrId) exObjId.alloc(0) else {
        val exLabelId = CompactBytes.toWrite(labelId).after(exObjId)
        val exPropId = CompactBytes.toWrite(propId).after(exLabelId)
        exLabelId.write(labelId, exLabelId.write(propId, exPropId.alloc(0)))
      })
    })
  }
  def value[Value](attrId: RawAttr[Value], value: Value, valueSrcId: ObjId) =
    OuterRawValueConverter.allocWrite[Value](0,attrId,value,valueSrcId,hasIdAfter=true,allowEmptyValue=true)
  def valueFromBytes[Value](attrId: RawAttr[Value], b: RawValue): Value = {
    if(b.length==0) return attrId.converter.convert()
    val exchangeA = CompactBytes.toReadAt(b,0)
    val exchangeB = CompactBytes.toReadAfter(b,exchangeA)
    val exchangeC = CompactBytes.toReadAfter(b,exchangeB).checkIsLastIn(b) //exchangeC.readLong(b)
    exchangeA.head match {
      case CompactBytes.`strHead` if exchangeB.isSplitter =>
        attrId.converter.convert(exchangeA.readString(b))
      case _ => attrId.converter.convert(exchangeA.readLong(b), exchangeB.readLong(b))
    }
  }/*
  def keyFromBytes(key: RawKey): (Long,Long) = {
    val exHead = CompactBytes.toReadAt(key, 0)
    if(exHead.readLong(key) != head) Never()
    val exObjId = CompactBytes.toReadAfter(key, exHead)
    val exAttrId = CompactBytes.toReadAfter(key, exObjId).checkIsLastIn(key)
    (exObjId.readLong(key), exAttrId.readLong(key))
  }*/
}

class RawSearchConverterImpl extends RawSearchConverter {
  def head = 1L
  def key[Value](attrId: RawAttr[Value], value: Value, objId: ObjId): RawKey =
    key(attrId, value, objId, hasObjId=true)
  def keyWithoutObjId[Value](attrId: RawAttr[Value], value: Value): RawKey =
    key(attrId, value, 0L, hasObjId=false)
  private def key[Value](attrId: RawAttr[Value], value: Value, objId: ObjId, hasObjId: Boolean): RawKey = {
    val labelId = attrId.labelId
    val propId = attrId.propId
    val exHead = CompactBytes.toWrite(head).at(0)
    val exLabelId = CompactBytes.toWrite(labelId).after(exHead)
    val exPropId = CompactBytes.toWrite(propId).after(exLabelId)
    val valuePos = exPropId.nextPos
    exHead.write(head, exLabelId.write(labelId, exPropId.write(propId,
      OuterRawValueConverter.allocWrite(valuePos, attrId, value, objId, hasObjId, allowEmptyValue = false)
    )))
  }
  def value(on: Boolean): Array[Byte] = if(!on) Array[Byte]() else {
    val value = 1L
    val exchangeA = CompactBytes.toWrite(value).at(0)
    val b = exchangeA.alloc(0)
    exchangeA.write(value,b)
    b
  }
}

object OuterRawValueConverter {
  def allocWrite[Value](
    spaceBefore: Int, attrId: RawAttr[Value], dbValue: Value,
    idAfter: ObjId, hasIdAfter: Boolean, allowEmptyValue: Boolean
  ): Array[Byte] = {
    val converter = attrId.converter
    if(!converter.nonEmpty(dbValue)){
      if(allowEmptyValue) new Array[Byte](spaceBefore) else Never()
    }
    if(!hasIdAfter) return converter.allocWrite(spaceBefore, dbValue, 0)
    val absEx = CompactBytes.toWrite(idAfter)
    val res = converter.allocWrite(spaceBefore, dbValue, absEx.size)
    absEx.atTheEndOf(res).write(idAfter, res)
  }
}

object InnerRawValueConverterImpl extends InnerRawValueConverter {
  def allocWrite(spaceBefore: Int, valueA: Long, valueB: Long, spaceAfter: Int) = {
    val exchangeA = CompactBytes.toWrite(valueA).at(spaceBefore)
    val exchangeB = CompactBytes.toWrite(valueB).after(exchangeA)
    exchangeA.write(valueA, exchangeB.write(valueB, exchangeB.alloc(spaceAfter)))
  }
  def allocWrite(spaceBefore: Int, value: String, spaceAfter: Int) = {
    val src = Bytes(value)
    val exchangeA = CompactBytes.toWrite(src).at(spaceBefore)
    val exchangeB = CompactBytes.`splitter`.after(exchangeA)
    exchangeA.write(src, exchangeB.writeHead(exchangeB.alloc(spaceAfter)))
  }
}

// matching ////////////////////////////////////////////////////////////////////

object BytesSame {
  def part(a: Array[Byte], b: Array[Byte], len: Int): Boolean = {
    if (a.length < len || b.length < len) return false
    var i = 0
    while (i < len) {
      if (a(i) != b(i)) return false
      i += 1
    }
    true
  }
}

/*
trait ValueObjFeed {
  def apply(valueA: Long, valueB: Long, objId: ObjId): Boolean
  def apply(value: String, objId: ObjId): Boolean
}
class ObjIdExtractor extends RawKeyExtractor[ValueObjFeed] {
  def apply(keyPrefix: RawKey, key: RawKey, feed: ValueObjFeed): Boolean = {
    if(!BytesSame.part(keyPrefix, key, keyPrefix.length)) return false
    val ex = CompactBytes.toReadAt(key, keyPrefix.length).checkIsLastIn(key)
    feed(ex.readLong(key), 0L)
  }
}
class AttrIdExtractor extends RawKeyExtractor[AttrFeed] {
  def apply(keyPrefix: RawKey, key: RawKey, feed: AttrFeed): Boolean = {
    if(!BytesSame.part(keyPrefix, key, keyPrefix.length)) return false
    val exLabelId = CompactBytes.toReadAt(key, keyPrefix.length)
    val exPropId = CompactBytes.toReadAfter(key, exLabelId).checkIsLastIn(key)
    feed(exLabelId.readLong(key), exPropId.readLong(key))
  }
}
*/


// bytes ///////////////////////////////////////////////////////////////////////

object BytesToBits {
  def apply(v: Int) = java.lang.Byte.SIZE * v
}

object LongByteExchange {
  // format: ihsspppp
  private def initSz = 1
  private def headSz = 1
  private def sizeSz = 2
  private def posSz = 4
  private def initOffset = BytesToBits(headSz+sizeSz+posSz)
  private def headOffset = BytesToBits(sizeSz+posSz)
  private def sizeOffset = BytesToBits(posSz)
  def apply(head: Int, size: Int, init: Int): LongByteExchange =
    new LongByteExchange(
      LongFits(init, BytesToBits(initSz), isUnsigned = false, initOffset) |
        LongFits(head, BytesToBits(headSz), isUnsigned = true, headOffset) |
        LongFits(size, BytesToBits(sizeSz), isUnsigned = true, sizeOffset)
    )
  def apply(array: Array[Long], index: Int) = new LongByteExchange(array(index))
}

class LongByteExchange private (val value: Long) extends AnyVal {
  import LongByteExchange._

  private def headMask = 0xFF
  private def sizeMask = 0xFFFF
  private def posMask: Long = 0xFFFFFFFFL

  final def init: Int = (value >> initOffset).toInt //signed
  final def head: Int = {
    val res = (value >> headOffset).toInt & headMask
    if(res < 0) Never()
    res
  }
  final def size: Int = {
    val res = (value >> sizeOffset).toInt & sizeMask
    if(res <= 0) Never()
    res
  }
  private def pos: Int = {
    val res = value.toInt
    if(res < 0) Never()
    res
  }
  final def nextPos = pos + size
  final def at(v: Int) = new LongByteExchange((value & ~posMask) | v)
  final def after(prev: LongByteExchange) = at(prev.nextPos)
  final def atTheEndOf(b: Array[Byte]) = at(b.length - size)

  final def readLong(b: Array[Byte]): Long = {
    CompactBytes.checkLongInfo(head)
    var i = pos + 1
    val to = nextPos
    var l: Long = init
    while (i < to) {
      l = (l << BytesToBits(1)) | (b(i) & 0xFFL)
      i += 1
    }
    l
  }
  final def write(value: Long, b: Array[Byte]): Array[Byte] = {
    var l = value
    var i = nextPos - 1
    while (i > pos) {
      b(i) = l.toByte
      l >>= BytesToBits(1)
      i -= 1
    }
    writeHead(b)
  }

  final def write(src: Array[Byte], dst: Array[Byte]): Array[Byte] = {
    if(src.length != size - CompactBytes.headSize) Never()
    System.arraycopy(src, 0, dst, pos + CompactBytes.headSize, src.length)
    writeHead(dst)
  }
  final def readString(b: Array[Byte]): String =
    new String(b, pos + CompactBytes.headSize, size - CompactBytes.headSize, UTF_8)

  final def writeHead(dst: Array[Byte]): Array[Byte] = {
    dst(pos) = head.toByte
    dst
  }

  final def isSplitter = CompactBytes.`splitterHead` == head

  final def checkIsLastIn(b: Array[Byte]): LongByteExchange = {
    if(b.length != nextPos) Never()
    this
  }

  final def alloc(spaceAfter: Int) = new Array[Byte](nextPos + spaceAfter)
}


object CompactBytes {
  final def headSize = 1

  private def minVarSize = headSize + 1

  private def varSizeCount = 8

  private def byteValueCount = 0x100

  private val headToInfo = new Array[Long](byteValueCount)
  private val negativeSizeToInfo = new Array[Long](minVarSize + varSizeCount)
  private val positiveSizeToInfo = new Array[Long](minVarSize + varSizeCount)
  private val byteValueToInfo = new Array[Long](byteValueCount)
  private val shortHiByteValueToInfo = new Array[Long](byteValueCount)

  final val `strHead` = 0xF9
  final val `splitterHead` = 0

  private def splitterPos(b: Array[Byte], pos: Int): Int = {
    var p = pos
    while(p < b.length && b(p) != `splitterHead`) p += 1
    p
  }

  final def checkLongInfo(head: Int): LongByteExchange = {
    val info = LongByteExchange(headToInfo, head)
    if (info.value == 0) Never()
    info
  }

  final def toReadAfter(b: Array[Byte], prev: LongByteExchange) =
    toReadAt(b,prev.nextPos)
  final def toReadAt(b: Array[Byte], pos: Int): LongByteExchange = (UInt(b,pos) match {
    case hd@`splitterHead` => `splitter`
    case hd@`strHead` => LongByteExchange(hd, splitterPos(b, pos) - pos, 0)
    case hd => checkLongInfo(hd)
  }).at(pos)

  private def fit(v: Long) =
    java.lang.Long.SIZE / java.lang.Byte.SIZE -
      java.lang.Long.numberOfLeadingZeros(v) / java.lang.Byte.SIZE

  final def toWrite(l: Long): LongByteExchange = if (l >= 0) {
    val fitIn = fit(l)
    if (fitIn <= 2) {
      if (fitIn < 2) {
        val info = LongByteExchange(byteValueToInfo, l.toInt)
        if (info.value != 0) return info
      }
      val info = LongByteExchange(shortHiByteValueToInfo, l.toInt >>> BytesToBits(1))
      if (info.value != 0) return info
    }
    val info = LongByteExchange(positiveSizeToInfo, headSize + fitIn)
    if (info.value != 0) return info
    Never()
  } else {
    val negativeFitIn = fit(~l)
    val fixedNegativeFitIn = if(negativeFitIn>0) negativeFitIn else 1 //if l = -1, then fit = 0, but minimum must be 1, we have no head for this
    val info = LongByteExchange(negativeSizeToInfo, headSize + fixedNegativeFitIn )
    if (info.value != 0) return info
    Never()
  }

  final def toWrite(value: Array[Byte]): LongByteExchange = {
    if(splitterPos(value, 0)!=value.length) Never()
    LongByteExchange(`strHead`, headSize + value.length, 0)
  }


  private def reg(dst: Array[Long], toKey: LongByteExchange=>Int, info: LongByteExchange): LongByteExchange = {
    val i = toKey(info)
    if (dst(i) != 0) Never()
    dst(i) = info.value
    info
  }
  private def reg(info: LongByteExchange): LongByteExchange = reg(headToInfo, _.head, info)

  for (v <- 0 to varSizeCount - 1)
    reg(negativeSizeToInfo, _.size, reg(LongByteExchange(0x0F - v, minVarSize + v, -1)))
  for (head <- 0x10 to 0x7F)
    reg(byteValueToInfo, _.init, reg(LongByteExchange(head, 1, head - 0x10)))
  for (head <- 0x80 to 0xEF)
    reg(shortHiByteValueToInfo, _.init, reg(LongByteExchange(head, 2, head - 0x80)))
  for (v <- 0 to varSizeCount - 1)
    reg(positiveSizeToInfo, _.size, reg(LongByteExchange(0xF0 + v, minVarSize + v, 0)))

  final val `splitter` = LongByteExchange(`splitterHead`, headSize, 0)
}
