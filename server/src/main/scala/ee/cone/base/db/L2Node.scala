
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

class NodeFactoryImpl(
  attr: AttrFactory,
  noObj: Obj,
  dbWrapType: WrapType[DBNode]
)(
  val dbNode: Attr[DBNode],
  val nonEmpty: Attr[Boolean],
  val noNode: Obj = noObj.wrap(dbWrapType, NoDBNode)
) extends NodeFactory with CoHandlerProvider {
  def toNode(tx: BoundToTx, objId: ObjId) = noObj.wrap(dbWrapType, new DBNodeImpl(objId)(tx))
  def handlers = List(
    CoHandler(GetValue(dbWrapType, dbNode))((outerObj,innerObj)⇒innerObj.data),
    CoHandler(GetValue(dbWrapType, nonEmpty))((outerObj,innerObj)⇒true)
  )
}
