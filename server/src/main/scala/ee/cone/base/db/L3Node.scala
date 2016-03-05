
package ee.cone.base.db

import ee.cone.base.db.Types._
import ee.cone.base.util.Never

case object NoDBNode extends DBNode {
  def nonEmpty = false
  def objId = Never()
  def apply[Value](attr: Attr[Value]) = Never()
  def update[Value](attr: Attr[Value], value: Value) = Never()
  def tx = Never()
}

case class DBNodeImpl(objId: Long)(val tx: BoundToTx) extends DBNode {
  def nonEmpty = true
  def apply[Value](attr: Attr[Value]) = attr.get(this)
  def update[Value](attr: Attr[Value], value: Value) = attr.set(this, value)
}

class NodeFactoryImpl extends NodeFactory {
  def noNode = NoDBNode
  def toNode(tx: BoundToTx, objId: ObjId) = new DBNodeImpl(objId)(tx)
  def seqNode(tx: BoundToTx) = toNode(tx,0L)
}