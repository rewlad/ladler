
package ee.cone.base.db

import ee.cone.base.util.{Never, Bytes, MD5}


class InnerFactIndex(
  rawFactConverter: RawFactConverter, tx: RawIndex
) extends Index {
  def update(objId: Long, attrId: Long, value: DBValue): Unit =
    tx.set(rawFactConverter.key(objId, attrId), rawFactConverter.value(value))
  def apply(objId: Long, attrId: Long): DBValue =
    rawFactConverter.valueFromBytes(tx.get(rawFactConverter.key(objId, attrId)))
}

class AppendOnlyIndex(db: InnerFactIndex) {
  def update(objId: Long, attrId: Long, value: DBValue): Unit = {
    if(db(objId, attrId) != DBRemoved) Never()
    db(objId, attrId) = value
  }
}

class InnerIndexIndex(
  rawIndexConverter: RawIndexConverter, tx: RawIndex, indexed: Long=>Boolean
) {
  def update(objId: Long, attrId: Long, value: DBValue, on: Boolean): Unit =
    if(indexed(attrId) && value != DBRemoved)
      tx.set(rawIndexConverter.key(attrId, value, objId), rawIndexConverter.value(on))
}

class RewritableTriggeringIndex(db: InnerFactIndex, search: InnerIndexIndex, attrCalcExecutor: AttrCalcExecutor) extends Index {
  def apply(objId: Long, attrId: Long) = db(objId, attrId)
  def update(objId: Long, attrId: Long, value: DBValue): Unit = {
    val wasValue = db(objId, attrId)
    if (value == wasValue) { return }
    search(objId, attrId, wasValue) = false
    db(objId, attrId) = value
    search(objId, attrId, value) = true
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
