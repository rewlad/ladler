
package ee.cone.base.db

import ee.cone.base.db.Types._
import ee.cone.base.util.Never

class FactIndexImpl(
  rewritable: Boolean,
  rawFactConverter: RawFactConverter,
  tx: RawIndex,
  srcNode: ()=>DBNode,
  rawVisitor: RawVisitor[AttrId]
) extends FactIndex {
  def get(objId: ObjId, attrId: AttrId) =
    rawFactConverter.valueFromBytes(tx.get(rawFactConverter.key(objId, attrId)))
  def updating(objId: ObjId, attrId: AttrId, value: DBValue): Option[()=>Unit] = {
    val wasValue = get(objId, attrId)
    if (value == wasValue) { return None }
    if (!rewritable && wasValue != DBRemoved) Never()
    val key = rawFactConverter.key(objId, attrId)
    val rawValue = rawFactConverter.value(value, srcNode().objId)
    Some(() => tx.set(key, rawValue))
  }
  def execute(objId: ObjId, feed: Feed[AttrId]): Unit = {
    val key = rawFactConverter.keyWithoutAttrId(objId)
    tx.seek(key)
    rawVisitor.execute(key, feed)
  }
}

