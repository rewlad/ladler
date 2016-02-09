
package ee.cone.base.db

import ee.cone.base.util.Never

class AttrIndexImpl(
  rawFactConverter: RawFactConverter, rawSearchConverter: RawSearchConverter, tx: RawIndex,
  attrId: AttrId, rewritable: Boolean, indexed: Boolean, calcList: ()=>List[AttrCalc]
) extends UpdatableAttrIndex[DBValue] {
  def apply(objId: ObjId): DBValue =
    rawFactConverter.valueFromBytes(tx.get(rawFactConverter.key(objId, attrId)))
  def update(objId: ObjId, value: DBValue): Unit = {
    val wasValue = apply(objId)
    if (value == wasValue) { return }

    if(!rewritable && wasValue != DBRemoved) Never()
    Inner(objId, wasValue) = false
    Inner(objId) = value
    if(indexed) Inner(objId, value) = true
    for(calc <- calcList()) calc.recalculate(objId)
  }
  private object Inner {
    def update(objId: ObjId, value: DBValue): Unit =
      tx.set(rawFactConverter.key(objId, attrId), rawFactConverter.value(value))
    def update(objId: ObjId, value: DBValue, on: Boolean): Unit =
      if(value != DBRemoved)
        tx.set(rawSearchConverter.key(attrId, value, objId), rawSearchConverter.value(on))
  }
}
