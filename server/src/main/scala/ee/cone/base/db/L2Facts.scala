
package ee.cone.base.db

import ee.cone.base.connection_api.CoHandlerLists
import ee.cone.base.db.Types._

class FactIndexImpl(
  rawFactConverter: RawFactConverter,
  rawVisitor: RawVisitor,
  calcLists: CoHandlerLists
) extends FactIndex {
  private var srcObjId = 0L
  def switchSrcObjId(objId: ObjId): Unit = srcObjId = objId
  def get[Value](node: DBNode, attr: Attr[Value]) = {
    val rawAttr = attr.rawAttr
    val key = rawFactConverter.key(node.objId, rawAttr)
    rawFactConverter.valueFromBytes(rawAttr, node.tx.rawIndex.get(key))
  }
  def set[Value](node: DBNode, attr: Attr[Value], value: Value): Unit = {
    val rawAttr = attr.rawAttr
    if (rawAttr.converter.same(get(node, attr),value)) { return }
    //if(calcList.isEmpty) throw new Exception(s"$attr is lost")
    for(calc <- calcLists.list(BeforeUpdate(attr.nonEmpty))) calc.handle(node)
    val key = rawFactConverter.key(node.objId, rawAttr)
    val rawValue = rawFactConverter.value(rawAttr, value, srcObjId)
    node.tx.rawIndex.set(key, rawValue)
    for(calc <- calcLists.list(AfterUpdate(attr.nonEmpty))) calc.handle(node)
  }
  def execute(node: DBNode, feed: Feed): Unit = {
    val key = rawFactConverter.keyWithoutAttrId(node.objId)
    node.tx.rawIndex.seek(key)
    rawVisitor.execute(node.tx.rawIndex, key, feed)
  }
}

