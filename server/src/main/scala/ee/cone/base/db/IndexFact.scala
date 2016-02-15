
package ee.cone.base.db

import ee.cone.base.util.Never

class FactIndex(
  rewritable: Boolean,
  rawFactConverter: RawFactConverter,
  tx: RawIndex,
  srcValueId: ()=>ObjId
){
  def apply(objId: ObjId, attrId: AttrId) =
    rawFactConverter.valueFromBytes(tx.get(rawFactConverter.key(objId, attrId)))
  def updating(objId: ObjId, attrId: AttrId, value: DBValue): Option[()=>Unit] = {
    val wasValue = apply(objId, attrId)
    if (value == wasValue) { return None }
    if (!rewritable && wasValue != DBRemoved) Never()
    val key = rawFactConverter.key(objId, attrId)
    val rawValue = rawFactConverter.value(value, srcValueId())
    Some(() => tx.set(key, rawValue))
  }
}

case class CalcIndexImpl(attrId: AttrId)(db: FactIndex) extends CalcIndex {
  private var affects: List[AttrCalc] = Nil
  protected def affects(calc: AttrCalc) = affects = calc :: affects
  def apply(objId: ObjId): DBValue = db(objId, attrId)
  def update(objId: ObjId, value: DBValue): Unit =
    db.updating(objId, attrId, value).foreach{ update =>
      for(calc <- affects) calc.beforeUpdate(objId)
      update()
      for(calc <- affects) calc.afterUpdate(objId)
    }
}
