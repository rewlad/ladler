
package ee.cone.base.db

import ee.cone.base.connection_api.{Obj, Attr, CoHandlerLists}
import ee.cone.base.db.Types._
import ee.cone.base.util.Never

class FactIndexImpl(
  rawFactConverter: RawFactConverter,
  rawVisitor: RawVisitor,
  calcLists: CoHandlerLists,
  nodeFactory: NodeFactory
) extends FactIndex {
  private var srcObjId = 0L
  def switchReason(node: Obj): Unit =
    srcObjId = if(node.nonEmpty) node(nodeFactory.objId) else 0L
  def get[Value](node: Obj, attr: RawAttr[Value]) = {
    val key = rawFactConverter.key(node(nodeFactory.objId), attr)
    val rawIndex = node(nodeFactory.rawIndex)
    rawFactConverter.valueFromBytes(attr.converter, rawIndex.get(key))
  }
  def set[Value](node: Obj, attr: Attr[Value] with RawAttr[Value], value: Value): Unit = {
    if (get(node, attr) == value) { return } // we can't fail on empty values
    //if(calcList.isEmpty) throw new Exception(s"$attr is lost")
    for(calc <- calcLists.list(BeforeUpdate(attr.defined))) calc(node)
    val rawIndex = node(nodeFactory.rawIndex)
    val key = rawFactConverter.key(node(nodeFactory.objId), attr)
    val rawValue = rawFactConverter.value(attr, value, srcObjId)
    rawIndex.set(key, rawValue)
    for(calc <- calcLists.list(AfterUpdate(attr.defined))) calc(node)
  }
  def execute(node: Obj, feed: Feed): Unit = {
    val key = rawFactConverter.keyWithoutAttrId(node(nodeFactory.objId))
    val rawIndex = node(nodeFactory.rawIndex)
    rawIndex.seek(key)
    rawVisitor.execute(rawIndex, key, key.length, feed)
  }
}

