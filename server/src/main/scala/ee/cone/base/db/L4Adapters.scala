package ee.cone.base.db

import ee.cone.base.util.Never

case class RuledIndexAdapterImpl[Value](
  ruled: CalcIndex
)(
  val converter: DBValueConverter[Value]
) extends RuledIndexAdapter[Value] {
  def set(node: DBNode, value: Value) = node(ruled) = converter(value)
  def get(node: DBNode) = converter(node(ruled))
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
