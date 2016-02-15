
package ee.cone.base.db

import ee.cone.base.util.Never

class FactIndex(
  rewritable: Boolean,
  rawFactConverter: RawFactConverter,
  tx: RawIndex,
  srcNode: ()=>DBNode
){
  def apply(node: DBNode, attrId: AttrId) =
    rawFactConverter.valueFromBytes(tx.get(rawFactConverter.key(node.objId, attrId)))
  def updating(node: DBNode, attrId: AttrId, value: DBValue): Option[()=>Unit] = {
    val wasValue = apply(node, attrId)
    if (value == wasValue) { return None }
    if (!rewritable && wasValue != DBRemoved) Never()
    val key = rawFactConverter.key(node.objId, attrId)
    val rawValue = rawFactConverter.value(value, srcNode().objId)
    Some(() => tx.set(key, rawValue))
  }
}

case class CalcIndexImpl(attrId: AttrId)(db: FactIndex) extends CalcIndex {
  private var affects: List[AttrCalc] = Nil
  def affects(calc: AttrCalc) = affects = calc :: affects
  def get(node: DBNode): DBValue = db(node, attrId)
  def set(node: DBNode, value: DBValue): Unit =
    db.updating(node, attrId, value).foreach{ update =>
      for(calc <- affects) calc.beforeUpdate(node)
      update()
      for(calc <- affects) calc.afterUpdate(node)
    }
}
