package ee.cone.base.db

import ee.cone.base.connection_api.{EventKey, LifeCycle, BaseCoHandler}
import ee.cone.base.db.Types._

trait Attr[Value] {
  def defined: Attr[Boolean]
  def ref: Attr[Ref[Value]]
  def get(node: DBNode): Value
  def set(node: DBNode, value: Value): Unit
  def rawAttr: RawAttr[Value]
}

trait DBNode {
  def nonEmpty: Boolean
  def objId: Long
  def tx: RawTx
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
  def handlers[Value](labelAttr: Attr[_], propAttr: Attr[Value]): List[BaseCoHandler]
}
case class SearchByLabelProp[Value](label: Attr[Boolean], prop: Attr[Boolean])
  extends EventKey[SearchRequest[Value],Unit]
class SearchRequest[Value](
  val tx: RawTx, val value: Value, val objId: Option[ObjId], val feed: Feed
)

case class BeforeUpdate(attr: Attr[Boolean]) extends EventKey[DBNode,Unit]
case class AfterUpdate(attr: Attr[Boolean]) extends EventKey[DBNode,Unit]

class RawTx(val lifeCycle: LifeCycle, val rw: Boolean, val rawIndex: RawIndex, val commit: ()=>Unit)
