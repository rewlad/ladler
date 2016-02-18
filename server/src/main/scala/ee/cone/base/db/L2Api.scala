package ee.cone.base.db

import ee.cone.base.connection_api.ConnectionComponent
import ee.cone.base.db.Types._

trait FactIndex {
  def get(objId: ObjId, attrId: AttrId): DBValue
  def set(objId: ObjId, attrId: AttrId, value: DBValue): Unit
  def execute(objId: ObjId, feed: Feed[AttrId]): Unit
}

trait SearchIndex {
  def execute(attrId: AttrId, value: DBValue, feed: Feed[ObjId]): Unit
  def execute(attrId: AttrId, value: DBValue, objId: ObjId, feed: Feed[ObjId]): Unit
  def composeAttrId(labelAttrId: AttrId, propAttrId: AttrId): AttrId
  def attrCalc(attrId: AttrId): SearchAttrCalc
}

trait AttrCalc extends ConnectionComponent {
  def affectedBy: List[AttrId]
  def beforeUpdate(objId: ObjId): Unit
  def afterUpdate(objId: ObjId): Unit
}

trait SearchAttrCalc extends AttrCalc {
  def searchAttrId: AttrId
}