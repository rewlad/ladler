package ee.cone.base.db

import ee.cone.base.connection_api._
import ee.cone.base.db.Types._

class ProtectedBoundToTx[DBEnvKey](val rawIndex: RawIndex, var enabled: Boolean) extends BoundToTx // not case

trait BoundToTx

trait DBNode {
  def nonEmpty: Boolean
  def objId: ObjId
  def tx: BoundToTx
  def rawIndex: RawIndex
  def nextObjId: ObjId
}

trait NodeFactory {
  def noNode: Obj
  def toNode(tx: BoundToTx, objId: ObjId): Obj
  def dbNode: Attr[DBNode]
  def nonEmpty: Attr[Boolean]
}

trait AttrFactory {
  def noAttr: Attr[Boolean]
  def apply[V](hiAttrId: HiAttrId, loAttrId: LoAttrId, valueType: AttrValueType[V]): Attr[V] with RawAttr[V]
  def apply[V](uuid: String, valueType: AttrValueType[V]): Attr[V] with RawAttr[V]
  def derive[V](attrA: Attr[Boolean], attrB: Attr[V]): Attr[V] with RawAttr[V]
  def defined(attr: Attr[_]): Attr[Boolean]
}

trait FactIndex {
  def switchReason(node: Obj): Unit
  def execute(node: Obj, feed: Feed): Unit
  def handlers[Value](attr: Attr[Value]): List[BaseCoHandler]
}

trait SearchIndex {
  def handlers[Value](labelAttr: Attr[_], propAttr: Attr[Value]): List[BaseCoHandler]
}
case class SearchByLabelProp[Value](label: Attr[Boolean], prop: Attr[Boolean])
  extends EventKey[SearchRequest[Value]=>Unit]
class SearchRequest[Value](
  val tx: BoundToTx, val value: Value, val objId: Option[ObjId], val feed: Feed
)

case class BeforeUpdate(attr: Attr[Boolean]) extends EventKey[Obj=>Unit]
case class AfterUpdate(attr: Attr[Boolean]) extends EventKey[Obj=>Unit]
trait OnUpdate {
  //invoke will be called before and after update if all attrs are defined
  def handlers(definedAttrs: List[Attr[Boolean]], invoke: (Boolean,Obj) ⇒ Unit): List[BaseCoHandler]
}

case class ToRawValueConverter[Value](valueType: AttrValueType[Value])
  extends EventKey[RawValueConverter[Value]]