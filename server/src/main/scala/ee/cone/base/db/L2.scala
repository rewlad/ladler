package ee.cone.base.db

import ee.cone.base.connection_api._

trait BoundToTx

trait NodeAttrs {
  def objId: Attr[ObjId]
}

trait ObjIdFactory {
  def noObjId: ObjId
  def toObjId(uuid: String): ObjId
}

trait AttrFactory {
  def valueType[V](attr: Attr[V]): AttrValueType[V]
  def apply[V](uuid: String, valueType: AttrValueType[V]): Attr[V]
  def attrId[V](attr: Attr[V]): ObjId
  def handlers[Value](attr: Attr[Value])(get: (Obj,ObjId)⇒Value): List[BaseCoHandler]
}

trait FactIndex {
  def handlers[Value](attr: Attr[Value]): List[BaseCoHandler]
}

trait SearchRequest[Value]
case class SearchByLabelProp[Value](labelId: ObjId, labelType: AttrValueType[Obj], propId: ObjId, propType: AttrValueType[Value])
  extends EventKey[SearchRequest[Value]=>Unit]
trait SearchIndex {
  def create[Value](labelAttr: Attr[Obj], propAttr: Attr[Value]): SearchByLabelProp[Value]
  def handlers[Value](by: SearchByLabelProp[Value]): List[BaseCoHandler]
}

trait OnUpdate {//+
  //invoke will be called before and after update if all attrs are defined
  def handlers(need: List[Attr[_]], optional: List[Attr[_]])(invoke: (Boolean,Obj) ⇒ Unit): List[BaseCoHandler]
  //def handlers(needAttrIds: List[ObjId], optionalAttrIds: List[ObjId], invoke: (Boolean,Obj) ⇒ Unit): List[BaseCoHandler]
}
