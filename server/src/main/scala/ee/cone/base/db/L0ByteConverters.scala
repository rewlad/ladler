
package ee.cone.base.db

import java.nio.charset.StandardCharsets.UTF_8

import ee.cone.base.connection_api.ObjId
import ee.cone.base.util._
import ee.cone.base.db.Types._

import scala.annotation.tailrec

object RawDumpImpl extends RawDump {
  def apply(b: Array[Byte]) = {
    var pos = 0
    var res: List[Option[Object]] = Nil
    while(pos < b.length){
      val ex = CompactBytes.toReadAt(b,pos)
      ex.head match {
        case hd@CompactBytes.`splitterHead` => res = None :: res
        case hd@CompactBytes.`strHead` => res = Some(ex.readString(b)) :: res
        case hd => res = Some(java.lang.Long.valueOf(ex.readLong(b))) :: res
      }
      pos = ex.nextPos
    }
    res.reverse
  }
}

// converters //////////////////////////////////////////////////////////////////

class RawConverterImpl extends RawConverter {
  def head = 1L
  def toBytes(preId: ObjId, finId: ObjId) =
    toBytesInner(preId, valIdHas = false,    0L,    0L, valueHas = false, ""   , finId)
  def toBytes(preId: ObjId, valHi: Long, valLo: Long, finId: ObjId) =
    toBytesInner(preId, valIdHas = true , valHi, valLo, valueHas = false, ""   , finId)
  def toBytes(preId: ObjId, value: String, finId: ObjId) =
    toBytesInner(preId, valIdHas = false,    0L,    0L, valueHas = true , value, finId)

  private def toBytesInner(
    preId: ObjId, valIdHas: Boolean, valIdHi: Long, valIdLo: Long, valueHas: Boolean, value: String, finId: ObjId
  ): Array[Byte] = {
    val preIdHas = preId.nonEmpty
    val finIdHas = finId.nonEmpty

    val    headEx =              CompactBytes.toWrite(head).at(0)
    val preIdHiEx = if(preIdHas) CompactBytes.toWrite(preId.hi).after(   headEx) else    headEx
    val preIdLoEx = if(preIdHas) CompactBytes.toWrite(preId.lo).after(preIdHiEx) else    headEx
    val valIdHiEx = if(valIdHas) CompactBytes.toWrite(valIdHi ).after(preIdLoEx) else preIdLoEx
    val valIdLoEx = if(valIdHas) CompactBytes.toWrite(valIdLo ).after(valIdHiEx) else preIdLoEx
    val valueRaw  = if(valueHas) Bytes(value) else null
    val valueHiEx = if(valueHas) CompactBytes.toWrite(valueRaw).after(valIdLoEx) else valIdLoEx
    val valueLoEx = if(valueHas) CompactBytes.`splitter`       .after(valueHiEx) else valIdLoEx
    val finIdHiEx = if(finIdHas) CompactBytes.toWrite(finId.hi).after(valueLoEx) else valueLoEx
    val finIdLoEx = if(finIdHas) CompactBytes.toWrite(finId.lo).after(finIdHiEx) else valueLoEx

    val b = finIdLoEx.alloc(0)
    headEx.write(head, b)
    if(preIdHas){ preIdHiEx.write(preId.hi, b); preIdLoEx.write(preId.lo, b) }
    if(valIdHas){ valIdHiEx.write(valIdHi , b); valIdLoEx.write(valIdLo , b) }
    if(valueHas){ valueHiEx.write(valueRaw, b); valueLoEx.writeHead(      b) }
    if(finIdHas){ finIdHiEx.write(finId.hi, b); finIdLoEx.write(finId.lo, b) }
    b
  }
  @tailrec private def skip(b: Array[Byte], ex: LongByteExchange, count: Int): LongByteExchange =
    if(count > 0) skip(b, CompactBytes.toReadAfter(b,ex), count-1) else ex
  def fromBytes[Value](b: Array[Byte], skipBefore: Int, converter: RawValueConverter[Value], skipAfter: Int): Value = {
    if(b.length == 0){ return converter.convertEmpty() }
    var skipEx = skip(b, CompactBytes.toReadAt(b,0), skipBefore*2)
    val exchangeA = CompactBytes.toReadAfter(b,skipEx)
    val exchangeB = CompactBytes.toReadAfter(b,exchangeA)
    val lastEx = skip(b, exchangeB, skipAfter*2)
/*
    if(b.length != lastEx.nextPos) {
      println(RawDumpImpl(b))
      println(b.length)
      println(skipBefore,skipAfter)
      println(skipEx.size,skipEx.nextPos)
      println(exchangeA.size,exchangeA.nextPos)
      println(exchangeB.size,exchangeB.nextPos)
      println(lastEx.size,lastEx.nextPos)
    }
*/
    lastEx.checkIsLastIn(b)
    if(exchangeA.head == CompactBytes.`strHead` && exchangeB.isSplitter)
      converter.convert(exchangeA.readString(b))
    else converter.convert(exchangeA.readLong(b), exchangeB.readLong(b))
  }
}

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
