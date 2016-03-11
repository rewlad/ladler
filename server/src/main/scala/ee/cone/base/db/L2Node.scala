
package ee.cone.base.db

import ee.cone.base.connection_api.{Obj, BoundToTx, Attr}
import ee.cone.base.db.Types._
import ee.cone.base.util.Never

case object NoDBNode extends Obj {
  def nonEmpty = false
  def objId = Never()
  def apply[Value](attr: Attr[Value]) = Never()
  def update[Value](attr: Attr[Value], value: Value) = Never()
  def tx = Never()
}

case class DBNodeImpl(objId: ObjId)(val tx: ProtectedBoundToTx[_]) extends Obj {
  def nonEmpty = true
  def apply[Value](attr: Attr[Value]) = attr.get(this)
  def update[Value](attr: Attr[Value], value: Value) = attr.set(this, value)
}

case object ObjIdAttr extends Attr[ObjId] {
  def defined = Never()
  def set(node: Obj, value: ObjId) = Never()
  def get(node: Obj) = node.asInstanceOf[DBNodeImpl].objId
}

case object NextObjIdAttr extends Attr[ObjId] {
  def defined = Never()
  def set(node: Obj, value: ObjId) = Never()
  def get(node: Obj) = new ObjId(node.asInstanceOf[DBNodeImpl].objId.value + 1L)
}

case object RawIndexAttr extends Attr[RawIndex] {
  def defined = Never()
  def set(node: Obj, value: RawIndex) = Never()
  def get(node: Obj) = {
    val tx = node.asInstanceOf[DBNodeImpl].tx
    if(tx.enabled) tx.rawIndex else Never()
  }
}

class NodeFactoryImpl(
  val objId: Attr[ObjId] = ObjIdAttr,
  val nextObjId: Attr[ObjId] = NextObjIdAttr,
  val rawIndex: Attr[RawIndex] = RawIndexAttr
) extends NodeFactory {
  def noNode = NoDBNode
  def toNode(tx: BoundToTx, objId: ObjId) = new DBNodeImpl(objId)(tx.asInstanceOf[ProtectedBoundToTx[_]])
}