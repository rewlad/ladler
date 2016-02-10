
package ee.cone.base.db

import ee.cone.base.util.Never

case class RuledIndexImpl(
  inner: InnerUpdatableAttrIndexImpl, rewritable: Boolean, indexed: Boolean
)(
  val affects: ()=>List[AttrCalc]
) extends RuledIndex {
  def apply(objId: ObjId): DBValue = inner(objId)
  def update(objId: ObjId, value: DBValue): Unit = {
    val wasValue = inner(objId)
    if (value == wasValue) { return }

    if(!rewritable && wasValue != DBRemoved) Never()
    inner(objId, wasValue) = false
    inner(objId) = value
    if(indexed) inner(objId, value) = true
    for(calc <- affects()) calc.recalculate(objId)
  }
  def attrId = inner.attrId
}

case class InnerUpdatableAttrIndexImpl(
  attrId: AttrId
)(
  rawFactConverter: RawFactConverter, rawSearchConverter: RawSearchConverter,
  tx: RawIndex
){
  def apply(objId: ObjId): DBValue =
    rawFactConverter.valueFromBytes(tx.get(rawFactConverter.key(objId, attrId)))
  def update(objId: ObjId, value: DBValue): Unit =
    tx.set(rawFactConverter.key(objId, attrId), rawFactConverter.value(value))
  def update(objId: ObjId, value: DBValue, on: Boolean): Unit =
    if(value != DBRemoved)
      tx.set(rawSearchConverter.key(attrId, value, objId), rawSearchConverter.value(on))
}
