
package ee.cone.base.db

import ee.cone.base.util.Never

case class MainRuledIndex(
  inner: InnerUpdatableAttrIndexImpl, indexed: Boolean
) extends RuledIndex {
  private var affects: List[AttrCalc] = Nil
  protected def affects(calc: AttrCalc) = affects = calc :: affects
  def apply(objId: ObjId): DBValue = inner(objId)
  def update(objId: ObjId, value: DBValue): Unit = {
    val wasValue = inner(objId)
    if (value == wasValue) { return }
    inner(objId, wasValue) = false
    inner(objId) = value
    if(indexed) inner(objId, value) = true
    for(calc <- affects) calc.recalculate(objId)
  }
  def attrId = inner.attrId
}

case class InstantRuledIndex(
  inner: InnerUpdatableAttrIndexImpl, indexed: Boolean
) extends RuledIndex {
  protected def affects(calc: AttrCalc) = Never()
  def apply(objId: ObjId): DBValue = inner(objId)
  def update(objId: ObjId, value: DBValue): Unit = {
    val wasValue = inner(objId)
    if(wasValue != DBRemoved) Never()
    inner(objId) = value
    if(indexed) inner(objId, value) = true
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
