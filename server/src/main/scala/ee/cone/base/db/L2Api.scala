package ee.cone.base.db

import ee.cone.base.connection_api.ConnectionComponent
import ee.cone.base.db.Types._

trait FactIndex {
  def get[Value](objId: ObjId, attrId: Attr[Value]): Value
  def set[Value](objId: ObjId, attrId: Attr[Value], value: Value): Unit
  def execute(objId: ObjId, feed: Feed): Unit
}

trait SearchIndex {
  def execute[Value](attrId: Attr[Value], value: Value, feed: Feed): Unit

  def execute[Value](attrId: Attr[Value], value: Value, objId: ObjId, feed: Feed): Unit
  def composeAttrId[Value](labelAttrId: Attr[Boolean], propAttrId: Attr[Value]): Attr[Value]
  def attrCalc[Value](attrId: Attr[Value]): SearchAttrCalc[Value]
}

trait AttrCalc extends ConnectionComponent {
  def affectedBy: List[Attr[Boolean]]
  def beforeUpdate(objId: ObjId): Unit
  def afterUpdate(objId: ObjId): Unit
}

trait SearchAttrCalc[Value] extends AttrCalc {
  def searchAttrId: Attr[Value]
}