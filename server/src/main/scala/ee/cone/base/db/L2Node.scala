
package ee.cone.base.db

import ee.cone.base.connection_api._
import ee.cone.base.util.Never

class DBWrapType extends WrapType[DBNode]

case object NoDBNode extends DBNode {
  def nonEmpty = false
  def objId = Never()
  def tx = Never()
  def rawIndex = Never()
  def nextObjId = Never()
}
case class DBNodeImpl(objId: ObjId)(val tx: ProtectedBoundToTx[_]) extends DBNode {
  def nonEmpty = true
  def rawIndex = if(tx.enabled) tx.rawIndex else Never()
  def nextObjId = new ObjId(objId.value + 1L)
}

class NodeAttrsImpl(
  attr: AttrFactory,
  asDefined: AttrValueType[Boolean],
  asDBNode: AttrValueType[DBNode]
)(
  val dbNode: Attr[DBNode] = attr("848ca1e3-e36b-4f9b-a39d-bd6b1d9bad98", asDBNode),
  val nonEmpty: Attr[Boolean] = attr("1cc81826-a1c0-4045-ab2a-e2501b4a71fc", asDefined)
) extends NodeAttrs

class NodeFactoryImpl(
  at: NodeAttrsImpl,
  noObj: Obj,
  dbWrapType: WrapType[DBNode]
)(
  val noNode: Obj = noObj.wrap(dbWrapType, NoDBNode)
) extends NodeFactory with CoHandlerProvider {
  def toNode(tx: BoundToTx, objId: ObjId) = noObj.wrap(dbWrapType, new DBNodeImpl(objId)(tx.asInstanceOf[ProtectedBoundToTx[_]]))
  def handlers = List(
    CoHandler(GetValue(dbWrapType, at.dbNode))((obj,innerObj)⇒innerObj.data),
    CoHandler(GetValue(dbWrapType, at.nonEmpty))((obj,innerObj)⇒innerObj.data.nonEmpty)
  )
}
