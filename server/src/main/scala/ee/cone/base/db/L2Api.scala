package ee.cone.base.db

import ee.cone.base.connection_api.{EventKey, LifeCycle, BaseCoHandler}
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
  def execute[Value](tx: RawTx, attrId: Attr[Value], value: Value, feed: Feed): Unit
  def execute[Value](tx: RawTx, attrId: Attr[Value], value: Value, objId: ObjId, feed: Feed): Unit
  def attrCalc[Value](attrId: Attr[Value]): List[BaseCoHandler]
  def attrCalc[Value](labelAttr: Attr[Boolean], propAttr: Attr[Value]): (Attr[Value], List[BaseCoHandler])
}



case class BeforeUpdate(attr: Attr[Boolean]) extends EventKey[DBNode,Unit]
case class AfterUpdate(attr: Attr[Boolean]) extends EventKey[DBNode,Unit]

class RawTx(val lifeCycle: LifeCycle, val rw: Boolean, val rawIndex: RawIndex, val commit: ()=>Unit)
