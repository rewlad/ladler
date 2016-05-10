
package ee.cone.base.db

import ee.cone.base.connection_api._
import ee.cone.base.util.Never

class DBWrapType extends WrapType[DBNode]

case object NoDBNode extends DBNode {
  def nonEmpty = false
  def objId = Never()
  def tx = Never()
  def rawIndex = Never()
}
case class DBNodeImpl(objId: ObjId)(val tx: ProtectedBoundToTx[_]) extends DBNode {
  def nonEmpty = true
  def rawIndex = if(tx.enabled) tx.rawIndex else Never()
}

class NodeFactoryImpl(
  noObj: Obj,
  dbWrapType: WrapType[DBNode]
)(
  val objId: Attr[ObjId],
  val nextObjId: Attr[ObjId],
  val rawIndex: Attr[RawIndex],
  val boundToTx: Attr[BoundToTx],
  val nonEmpty: Attr[Boolean],
  val noNode: Obj = noObj.wrap(dbWrapType, NoDBNode)
) extends NodeFactory with CoHandlerProvider {
  def toNode(tx: BoundToTx, objId: ObjId) = noObj.wrap(dbWrapType, new DBNodeImpl(objId)(tx))
  def handlers = List(
    CoHandler(GetValue(dbWrapType, objId))((outerObj,innerObj)⇒innerObj.data.objId),
    CoHandler(GetValue(dbWrapType, nextObjId))((outerObj,innerObj)⇒new ObjId(outerObj(objId).value + 1L)),
    CoHandler(GetValue(dbWrapType, rawIndex))((outerObj,innerObj)⇒innerObj.data.rawIndex),
    CoHandler(GetValue(dbWrapType, boundToTx))((outerObj,innerObj)⇒innerObj.data.tx),
    CoHandler(GetValue(dbWrapType, nonEmpty))((outerObj,innerObj)⇒true)
  )
}
