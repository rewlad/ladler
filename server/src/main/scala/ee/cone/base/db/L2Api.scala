package ee.cone.base.db

import ee.cone.base.connection_api._
import ee.cone.base.db.Types._

class ProtectedBoundToTx[DBEnvKey](val rawIndex: RawIndex, var enabled: Boolean) extends BoundToTx // not case

trait NodeFactory {
  def noNode: Obj
  def toNode(tx: BoundToTx, objId: ObjId): Obj
  def objId: Attr[ObjId]
  def nextObjId: Attr[ObjId]
  def rawIndex: Attr[RawIndex]
}

trait AttrFactory {
  def noAttr: Attr[Boolean]
  def apply[V](labelId: LabelId, propId: PropId, converter: RawValueConverter[V]): Attr[V] with RawAttr[V]
  def apply[V](propId: PropId, converter: RawValueConverter[V]): Attr[V] with RawAttr[V]
}

trait FactIndex {
  def switchReason(node: Obj): Unit
  def get[Value](node: Obj, attr: RawAttr[Value]): Value
  def set[Value](node: Obj, attr: Attr[Value] with RawAttr[Value], value: Value): Unit
  def execute(node: Obj, feed: Feed): Unit
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

