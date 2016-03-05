
package ee.cone.base.db

import ee.cone.base.connection_api.CoHandlerLists
import ee.cone.base.db.Types._
import ee.cone.base.util.Never

class FactIndexImpl(
  rawFactConverter: RawFactConverter,
  rawVisitor: RawVisitor,
  calcLists: CoHandlerLists
) extends FactIndex {
  private var srcObjId = 0L
  private def toRawIndex(tx: BoundToTx) =
    if(tx.enabled) tx.asInstanceOf[ProtectedBoundToTx].rawIndex else Never()
  def switchSrcObjId(objId: ObjId): Unit = srcObjId = objId
  def get[Value](node: DBNode, attr: Attr[Value]) = {
    val rawAttr = attr.rawAttr
    val key = rawFactConverter.key(node.objId, rawAttr)
    val rawIndex = toRawIndex(node.tx)
    rawFactConverter.valueFromBytes(rawAttr, rawIndex.get(key))
  }
  def set[Value](node: DBNode, attr: Attr[Value], value: Value): Unit = {
    val rawAttr = attr.rawAttr
    if (get(node, attr) == value) { return }
    //if(calcList.isEmpty) throw new Exception(s"$attr is lost")
    for(calc <- calcLists.list(BeforeUpdate(attr.defined))) calc(node)
    val key = rawFactConverter.key(node.objId, rawAttr)
    val rawValue = rawFactConverter.value(rawAttr, value, srcObjId)
    val rawIndex = toRawIndex(node.tx)
    rawIndex.set(key, rawValue)
    for(calc <- calcLists.list(AfterUpdate(attr.defined))) calc(node)
  }
  def execute(node: DBNode, feed: Feed): Unit = {
    val key = rawFactConverter.keyWithoutAttrId(node.objId)
    val rawIndex = toRawIndex(node.tx)
    rawIndex.seek(key)
    rawVisitor.execute(rawIndex, key, feed)
  }
}

