package ee.cone.base.db

import ee.cone.base.connection_api.{EventKey, LifeCycle, BaseCoHandler}
import ee.cone.base.db.Types._

trait Attr[Value] {
  def defined: Attr[Boolean]
  def get(node: DBNode): Value
  def set(node: DBNode, value: Value): Unit
}

trait BoundToTx { def enabled: Boolean }
class ProtectedBoundToTx(val rawIndex: RawIndex, var enabled: Boolean) extends BoundToTx

trait DBNode {
  def nonEmpty: Boolean
  def objId: Long
  def tx: BoundToTx
  def apply[Value](attr: Attr[Value]): Value
  def update[Value](attr: Attr[Value], value: Value): Unit
}

trait Ref[Value] {
  def apply(): Value
  def update(value: Value): Unit
}

trait AttrFactory {
  def apply[V](labelId: Long, propId: Long, converter: RawValueConverter[V]): Attr[V] with RawAttr[V]
}

trait FactIndex {
  def switchSrcObjId(objId: ObjId): Unit
  def get[Value](node: DBNode, attr: RawAttr[Value]): Value
  def set[Value](node: DBNode, attr: Attr[Value] with RawAttr[Value], value: Value): Unit
  def execute(node: DBNode, feed: Feed): Unit
}

trait SearchIndex {
  def handlers[Value](labelAttr: Attr[_], propAttr: Attr[Value]): List[BaseCoHandler]
}
case class SearchByLabelProp[Value](label: Attr[Boolean], prop: Attr[Boolean])
  extends EventKey[SearchRequest[Value],Unit]
class SearchRequest[Value](
  val tx: BoundToTx, val value: Value, val objId: Option[ObjId], val feed: Feed
)

case class BeforeUpdate(attr: Attr[Boolean]) extends EventKey[DBNode,Unit]
case class AfterUpdate(attr: Attr[Boolean]) extends EventKey[DBNode,Unit]

