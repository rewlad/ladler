
package ee.cone.base.db

import ee.cone.base.util.Never
import ee.cone.base.db.LMTypes._

object LMFact {
  val `head` = 0L
  def key(objId: Long, attrId: Long): LMKey =
    key(objId, attrId, hasObjId=true, hasAttrId=true)
  def keyWithoutAttrId(objId: Long): LMKey =
    key(objId, 0, hasObjId=true, hasAttrId=false)
  def keyHeadOnly =
    key(0, 0, hasObjId=false, hasAttrId=false)
  private def key(objId: Long, attrId: Long, hasObjId: Boolean, hasAttrId: Boolean): LMKey = {
    val exHead = LMBytes.toWrite(`head`).at(0)
    exHead.write(`head`, if(!hasObjId) exHead.alloc(0) else {
      val exObjId = LMBytes.toWrite(objId).after(exHead)
      exObjId.write(objId, if(!hasAttrId) exObjId.alloc(0) else {
        val exAttrId = LMBytes.toWrite(attrId).after(exObjId)
        exAttrId.write(attrId, exAttrId.alloc(0))
      })
    })
  }
  def value(value: LMValue, valueSrcId: ValueSrcId): LMRawValue = {
    if(value == LMRemoved) return Array[Byte]()
    val exchangeSrcId = LMBytes.toWrite(valueSrcId)
    val b = value.allocWrite(0,exchangeSrcId.size)
    exchangeSrcId.atTheEndOf(b).write(valueSrcId, b)
  }
  def valueFromBytes(b: LMRawValue, check: Option[ValueSrcId⇒Boolean]): LMValue = {
    if(b.length==0) return LMRemoved
    val exchangeA = LMBytes.toReadAt(b,0)
    val exchangeB = LMBytes.toReadAfter(b,exchangeA)
    val exchangeC = LMBytes.toReadAfter(b,exchangeB).checkIsLastIn(b)
    if(check.nonEmpty && !check.get(exchangeC.readLong(b))) LMRemoved else exchangeA.head match {
      case LMBytes.`strHead` if exchangeB.isSplitter => LMStringValue(exchangeA.readString(b))
      case _ if exchangeB.isSplitter => LMLongValue(exchangeA.readLong(b))
      case _ => LMLongPairValue(exchangeA.readLong(b), exchangeB.readLong(b))
    }
  }
  def keyFromBytes(key: LMKey): (Long,Long) = {
    val exHead = LMBytes.toReadAt(key, 0)
    if(exHead.readLong(key) != LMFact.`head`) Never()
    val exObjId = LMBytes.toReadAfter(key, exHead)
    val exAttrId = LMBytes.toReadAfter(key, exObjId).checkIsLastIn(key)
    (exObjId.readLong(key), exAttrId.readLong(key))
  }
}

object LMIndex {
  val `head` = 1L
  def key(attrId: Long, value: LMValue, objId: Long): LMKey =
    key(attrId, value, objId, hasObjId=true)
  def keyWithoutObjId(attrId: Long, value: LMValue): LMKey =
    key(attrId, value, 0, hasObjId=false)
  private def key(attrId: Long, value: LMValue, objId: Long, hasObjId: Boolean): LMKey = {
    val exHead = LMBytes.toWrite(`head`).at(0)
    val exAttrId = LMBytes.toWrite(attrId).after(exHead)
    val valuePos = exAttrId.nextPos
    exHead.write(`head`, exAttrId.write(attrId,if(hasObjId){
      val absExObjId = LMBytes.toWrite(objId)
      val res = value.allocWrite(valuePos, absExObjId.size)
      val exObjId = absExObjId.atTheEndOf(res)
      exObjId.write(objId, res)
    }else{
      value.allocWrite(valuePos, 0)
    }))
  }
  def value(on: Boolean) = if(!on) Array[Byte]() else {
    val value = 1L
    val exchangeA = LMBytes.toWrite(value).at(0)
    val b = exchangeA.alloc(0)
    exchangeA.write(value,b)
    b
  }
}

object LMKey {
  def matchPrefix(keyPrefix: LMKey, key: LMKey) =
    BytesSame.part(keyPrefix, key, keyPrefix.length)
  def lastId(keyPrefix: LMKey, key: LMKey) =
    LMBytes.toReadAt(key, keyPrefix.length).checkIsLastIn(key).readLong(key)
}


