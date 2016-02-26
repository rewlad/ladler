package ee.cone.base.db

import ee.cone.base.connection_api.ConnectionComponent
import ee.cone.base.db.Types._

trait Attr[Value] {
  def nonEmpty: Attr[Boolean]
  def ref: Attr[Ref[Value]]
  def get(node: DBNode): Value
  def set(node: DBNode, value: Value): Unit
  def rawAttr: RawAttr[Value]
}

trait DBNode {
  def objId: Long
  def rawIndex: RawIndex
  def apply[Value](attr: Attr[Value]): Value
  def update[Value](attr: Attr[Value], value: Value): Unit
}

trait Ref[Value] {
  def apply(): Value
  def update(value: Value): Unit
}

trait AttrFactory {
  def apply[V](labelId: Long, propId: Long, converter: RawValueConverter[V]): Attr[V]
}

trait FactIndex {
  def switchSrcObjId(objId: ObjId): Unit
  def get[Value](node: DBNode, attrId: Attr[Value]): Value
  def set[Value](node: DBNode, attrId: Attr[Value], value: Value): Unit
  def execute(node: DBNode, feed: Feed): Unit
}

trait SearchIndex {
  def switchRawIndex(value: Option[RawIndex]): Unit
  def execute[Value](attrId: Attr[Value], value: Value, feed: Feed): Unit
  def execute[Value](attrId: Attr[Value], value: Value, objId: ObjId, feed: Feed): Unit
  def attrCalc[Value](attrId: Attr[Value]): SearchAttrCalc[Value]
  def attrCalc[Value](labelAttr: Attr[Boolean], propAttr: Attr[Value]): SearchAttrCalc[Value]
}

trait AttrCalc extends ConnectionComponent {
  def affectedBy: List[Attr[Boolean]]
  def beforeUpdate(node: DBNode): Unit
  def afterUpdate(node: DBNode): Unit
}

trait SearchAttrCalc[Value] extends AttrCalc {
  def searchAttrId: Attr[Value]
}