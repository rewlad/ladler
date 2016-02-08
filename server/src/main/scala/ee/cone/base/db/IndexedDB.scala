
package ee.cone.base.db

import ee.cone.base.util.Never

class IndexImpl(
  rawFactConverter: RawFactConverter, rawIndexConverter: RawIndexConverter,
  tx: RawIndex, attrUpdates: AttrId=>AttrUpdate
) extends Index {
  def apply(objId: ObjId, attrId: AttrId): DBValue =
    rawFactConverter.valueFromBytes(tx.get(rawFactConverter.key(objId, attrId)))
  def update(objId: ObjId, attrId: AttrId, value: DBValue): Unit = {
    val wasValue = apply(objId, attrId)
    if (value == wasValue) { return }
    val attrUpdate = attrUpdates(attrId)
    if(!attrUpdate.rewritable && wasValue != DBRemoved) Never()
    Inner(objId, attrId, wasValue) = false
    Inner(objId, attrId) = value
    if(attrUpdate.indexed) Inner(objId, attrId, value) = true
    for(calc <- attrUpdate.calcList) calc.recalculate(objId)
  }
  private object Inner {
    def update(objId: ObjId, attrId: AttrId, value: DBValue): Unit =
      tx.set(rawFactConverter.key(objId, attrId), rawFactConverter.value(value))
    def update(objId: ObjId, attrId: AttrId, value: DBValue, on: Boolean): Unit =
      if(value != DBRemoved)
        tx.set(rawIndexConverter.key(attrId, value, objId), rawIndexConverter.value(on))
  }
}
