package ee.cone.base.db

import ee.cone.base.db.Types.ObjId
import ee.cone.base.util.Never

class PropImpl[Value](ruled: CalcFactIndex, val converter: DBValueConverter[Value]) extends Prop[Value] {
  def set(node: DBNode, value: Value) = ruled.set(node.objId, converter(value))
  def get(node: DBNode) = converter(ruled.get(node.objId))
}

class NodeValueConverter extends DBValueConverter[Option[DBNode]] {
  override def apply(value: Option[DBNode]): DBValue = value match {
    case None => DBRemoved
    case Some(node) => DBLongValue(node.objId)
  }
  override def apply(value: DBValue): Option[DBNode] = value match {
    case DBRemoved => None
    case DBLongValue(v) => Some(new DBNodeImpl(v))
    case _ => Never()
  }
}

class DBNodeImpl(val objId: Long) extends DBNode {
  def apply[Value](attr: Prop[Value]) = attr.get(this)
  def update[Value](attr: Prop[Value], value: Value) = attr.set(this,value)
}

class ListByValue[Value](direct: Prop[Value], calcSearchIndex: CalcSearchIndex)  {
  def listByValue(value: Value): List[DBNode] = {
    val feed = new ListFeedImpl[ObjId,DBNode](objId=>new DBNodeImpl(objId))
    calcSearchIndex.execute(direct.converter(value), feed)
    feed.result.reverse
  }
  def listByValue(value: Value, fromNode: DBNode): List[DBNode] = {
    val feed = new ListFeedImpl[ObjId,DBNode](objId=>new DBNodeImpl(objId))
    calcSearchIndex.execute(direct.converter(value), fromNode.objId, feed)
    feed.result.reverse
  }
}

class ListByDBNode(inner: CalcFactIndexByObjId) {
  def apply(node: DBNode) = {
    val feed = new ListFeedImpl[CalcFactIndex,CalcFactIndex](identity)
    inner.execute(node.objId, feed)
    feed.result
  }
}

class ListFeedImpl[From,To](converter: From=>To) extends Feed[From] {
  var result: List[To] = Nil
  override def apply(value: From) = {
    result = converter(value) :: result
    true
  }
}