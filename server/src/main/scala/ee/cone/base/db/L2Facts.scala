
package ee.cone.base.db

import ee.cone.base.db.Types._
import ee.cone.base.util.Never

class FactIndexImpl(
  rewritable: Boolean,
  rawFactConverter: RawFactConverter,
  tx: RawIndex,
  srcNode: ()=>DBNode,
  rawVisitor: RawVisitor[AttrId],
  calcList: AttrId=>List[AttrCalc]
) extends FactIndex {
  def get(objId: ObjId, attrId: AttrId) =
    rawFactConverter.valueFromBytes(tx.get(rawFactConverter.key(objId, attrId)))
  def set(objId: ObjId, attrId: AttrId, value: DBValue): Unit = {
    val wasValue = get(objId, attrId)
    if (value == wasValue) { return }
    if (!rewritable && wasValue != DBRemoved) Never()
    val key = rawFactConverter.key(objId, attrId)
    val rawValue = rawFactConverter.value(value, srcNode().objId)
    for(calc <- calcList(attrId)) calc.beforeUpdate(objId)
    tx.set(key, rawValue)
    for(calc <- calcList(attrId)) calc.afterUpdate(objId)
  }
  def execute(objId: ObjId, feed: Feed[AttrId]): Unit = {
    val key = rawFactConverter.keyWithoutAttrId(objId)
    tx.seek(key)
    rawVisitor.execute(key, feed)
  }
}

class AttrCalcLists(attrInfoList: List[AttrCalc]) {
  lazy val value: Map[AttrId, List[AttrCalc]] =
    attrInfoList.flatMap { attrCalc ⇒
      attrCalc.affectedBy.map(attrId => (attrId, attrCalc))
    }.groupBy(_._1).mapValues(_.map(_._2))
}