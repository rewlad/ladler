package ee.cone.base.db

import ee.cone.base.db.Types._

trait FactIndex {
  def get(objId: ObjId, attrId: AttrId): DBValue
  def updating(objId: ObjId, attrId: AttrId, value: DBValue): Option[()=>Unit]
  def execute(objId: ObjId, feed: Feed[AttrId]): Unit
}

trait SearchIndex {
  def set(attrId: AttrId, value: DBValue, objId: ObjId, on: Boolean): Unit
  def execute(attrId: AttrId, value: DBValue, feed: Feed[ObjId]): Unit
  def execute(attrId: AttrId, value: DBValue, objId: ObjId, feed: Feed[ObjId]): Unit
}