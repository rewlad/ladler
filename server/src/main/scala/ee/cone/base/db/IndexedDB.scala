
package ee.cone.base.db

import ee.cone.base.util.{Never, Bytes, MD5}

class InnerIndex(
  rawFactConverter: RawFactConverter,
  rawIndexConverter: RawIndexConverter,
  tx: RawTx,
  indexed: Long=>Boolean
) extends Index {
  def update(objId: Long, attrId: Long, value: DBValue, on: Boolean): Unit =
    if(indexed(attrId) && value != DBRemoved)
      tx.set(rawIndexConverter.key(attrId, value, objId), rawIndexConverter.value(on))
  def update(objId: Long, attrId: Long, value: DBValue): Unit =
    tx.set(rawFactConverter.key(objId, attrId), rawFactConverter.value(value))
  def apply(objId: Long, attrId: Long): DBValue =
    rawFactConverter.valueFromBytes(tx.get(rawFactConverter.key(objId, attrId)))
}

class AppendOnlyIndex(db: InnerIndex) extends Index {
  def apply(objId: Long, attrId: Long) = db(objId, attrId)
  def update(objId: Long, attrId: Long, value: DBValue): Unit = {
    if(db(objId, attrId) != DBRemoved) Never()
    db(objId, attrId) = value
    db(objId, attrId, value) = true
  }
}

class RewritableTriggeringIndex(db: InnerIndex, attrCalcExecutor: AttrCalcExecutor) extends Index {
  def apply(objId: Long, attrId: Long) = db(objId, attrId)
  def update(objId: Long, attrId: Long, value: DBValue): Unit = {
    val wasValue = db(objId, attrId)
    if (value == wasValue) { return }
    db(objId, attrId, wasValue) = false
    db(objId, attrId) = value
    db(objId, attrId, value) = true
    attrCalcExecutor(objId, attrId)
  }
}

class AttrCalcExecutor(attrInfoList: List[AttrInfo]) {
  private lazy val calcListByAttrId: Map[Long, List[AttrCalc]] =
    attrInfoList.collect { case attrCalc: AttrCalc ⇒
      attrCalc.affectedByAttrIds.map(attrId => (attrId, attrCalc))
    }.flatten.groupBy(_._1).mapValues(_.map(_._2))
  def apply(objId: Long, attrId: Long): Unit =
    for(calcList <- calcListByAttrId.get(attrId); calc <- calcList)
      calc.recalculate(objId)
}
