package ee.cone.base.db

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
  def nonEmpty: Attr[Boolean]
}

trait ObjIdFactory {
  def noObjId: ObjId
  def toObjId(hiObjId: Long, loObjId: Long): ObjId
}

trait NodeFactory {
  def noNode: Obj
  def toNode(objId: ObjId): Obj
}

trait AttrFactory {
  def noAttr: Attr[Boolean]
  def apply[V](hiAttrId: Long, loAttrId: Long, valueType: AttrValueType[V]): Attr[V] with ObjId
  def apply[V](uuid: String, valueType: AttrValueType[V]): Attr[V] with ObjId
  def derive[V](attrA: Attr[Boolean], attrB: Attr[V]): Attr[V] with ObjId
  def defined(attr: Attr[_]): Attr[Boolean]
}

class AttrValueType[Value]
trait RawAttr[Value] {
  def valueType: AttrValueType[Value]
}

trait FactIndex {
  def switchReason(node: Obj): Unit
  def execute(obj: Obj)(feed: Attr[Boolean]⇒Boolean): Unit
  def handlers[Value](attr: Attr[Value]): List[BaseCoHandler]
}

trait SearchIndex {
  def handlers[Value](labelAttr: Attr[_], propAttr: Attr[Value]): List[BaseCoHandler]
}
case class SearchByLabelProp[Value](label: Attr[Boolean], prop: Attr[Boolean])
  extends EventKey[SearchRequest[Value]=>Unit]
class SearchRequest[Value](
  val tx: BoundToTx,
  val value: Value, val onlyThisValue: Boolean,
  val objId: ObjId, val feed: ObjId⇒Boolean
)

case class BeforeUpdate(attr: Attr[Boolean]) extends EventKey[Obj=>Unit]
case class AfterUpdate(attr: Attr[Boolean]) extends EventKey[Obj=>Unit]
trait OnUpdate {
  //invoke will be called before and after update if all attrs are defined
  def handlers(definedAttrs: List[Attr[Boolean]], invoke: (Boolean,Obj) ⇒ Unit): List[BaseCoHandler]
}

case class ToRawValueConverter[Value](valueType: AttrValueType[Value])
  extends EventKey[RawValueConverter[Value]]