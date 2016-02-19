package ee.cone.base.db

import ee.cone.base.connection_api.ConnectionComponent
import ee.cone.base.db.Types._

trait FactIndex {
  def get[Value](objId: ObjId, attrId: AttrId[Value]): Value
  def set[Value](objId: ObjId, attrId: AttrId[Value], value: Value): Unit
  def execute(objId: ObjId, feed: Feed): Unit
}

trait SearchIndex {
  def execute[Value](attrId: AttrId[Value], value: Value, feed: Feed): Unit

  def execute[Value](attrId: AttrId[Value], value: Value, objId: ObjId, feed: Feed): Unit
  def composeAttrId[Value](labelAttrId: AttrId[Boolean], propAttrId: AttrId[Value]): AttrId[Value]
  def attrCalc[Value](attrId: AttrId[Value]): SearchAttrCalc[Value]
}

trait AttrCalc extends ConnectionComponent {
  def affectedBy: List[AttrId[Boolean]]
  def beforeUpdate(objId: ObjId): Unit
  def afterUpdate(objId: ObjId): Unit
}

trait SearchAttrCalc[Value] extends AttrCalc {
  def searchAttrId: AttrId[Value]
}