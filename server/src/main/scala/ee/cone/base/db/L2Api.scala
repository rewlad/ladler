package ee.cone.base.db

import java.util.UUID

import ee.cone.base.connection_api._
import ee.cone.base.db.Types._

trait BoundToTx

case object TxSelectorKey extends EventKey[TxSelector]
trait TxSelector {
  def txOf(obj: Obj): BoundToTx
  def rawIndex(objId: ObjId): RawIndex
  def rawIndex(tx: BoundToTx): RawIndex
}

trait NodeAttrs {
  def objId: Attr[ObjId]
}

trait ObjIdFactory {
  def noObjId: ObjId
  def toObjId(hiObjId: Long, loObjId: Long): ObjId
  def toObjId(uuid: UUID): ObjId
  def toObjId(uuid: String): ObjId
  def compose(objIds: List[ObjId]): ObjId
  def toUUIDString(objId: ObjId): String
}

trait AttrFactory {
  def apply[V](uuid: String, valueType: AttrValueType[V]): Attr[V]
  def define[V](attrId: ObjId, valueType: AttrValueType[V]): Attr[V]
  def attrId[V](attr: Attr[V]): ObjId
  def valueType[V](attr: Attr[V]): AttrValueType[V]
  def toAttr[V](attrId: ObjId, valueType: AttrValueType[V]): Attr[V]
  def converter[V](valueType: AttrValueType[V]): RawValueConverter[V]
  def handlers[Value](attr: Attr[Value])(get: (Obj,ObjId)⇒Value): List[BaseCoHandler]
}

case class AttrValueType[Value](id: ObjId)

trait FactIndex {
  def switchReason(node: Obj): Unit
  def execute(obj: Obj)(feed: ObjId⇒Boolean): Unit
  def handlers[Value](attr: Attr[Value]): List[BaseCoHandler]
  def defined(attrId: ObjId): Attr[Boolean]
}

trait SearchIndex {
  def create[Value](labelAttr: Attr[Obj], propAttr: Attr[Value]): SearchByLabelProp[Value]
  def handlers[Value](by: SearchByLabelProp[Value]): List[BaseCoHandler]
}
case class SearchByLabelProp[Value](labelId: ObjId, labelType: AttrValueType[Obj], propId: ObjId, propType: AttrValueType[Value])
  extends EventKey[SearchRequest[Value]=>Unit]
class SearchRequest[Value](
  val tx: BoundToTx,
  val value: Value, val onlyThisValue: Boolean,
  val objId: ObjId, val feed: ObjId⇒Boolean
)

case class BeforeUpdate(attrId: ObjId) extends EventKey[Obj=>Unit]
case class AfterUpdate(attrId: ObjId) extends EventKey[Obj=>Unit]
trait OnUpdate {
  //invoke will be called before and after update if all attrs are defined
  def handlers(need: List[Attr[_]], optional: List[Attr[_]])(invoke: (Boolean,Obj) ⇒ Unit): List[BaseCoHandler]
  //def handlers(needAttrIds: List[ObjId], optionalAttrIds: List[ObjId], invoke: (Boolean,Obj) ⇒ Unit): List[BaseCoHandler]
}
case class ToRawValueConverter[Value](valueType: AttrValueType[Value])
  extends EventKey[RawValueConverter[Value]]
