
package ee.cone.base.db

import ee.cone.base.util.Never

case class RuledIndexImpl(attrId: AttrId, rewritable: Boolean)(
  rawFactConverter: RawFactConverter, tx: RawIndex, srcValueId: ()=>ObjId
) extends RuledIndex {
  private var affects: List[AttrCalc] = Nil
  protected def affects(calc: AttrCalc) = affects = calc :: affects
  def apply(objId: ObjId): DBValue =
    rawFactConverter.valueFromBytes(tx.get(rawFactConverter.key(objId, attrId)))
  def update(objId: ObjId, value: DBValue): Unit = {
    val wasValue = apply(objId)
    if (value == wasValue) { return }
    if (!rewritable && wasValue != DBRemoved) Never()
    for(calc <- affects) calc.beforeUpdate(objId)
    tx.set(rawFactConverter.key(objId, attrId), rawFactConverter.value(value, srcValueId()))
    for(calc <- affects) calc.afterUpdate(objId)
  }
}
