
package ee.cone.base.db

import ee.cone.base.connection_api.{ConnectionComponent, Registration}
import ee.cone.base.db.Types._

class FactIndexImpl(
  rawFactConverter: RawFactConverter,
  rawVisitor: RawVisitor,
  calcLists: AttrCalcLists
) extends FactIndex {
  private var srcObjId = 0L
  def switchSrcObjId(objId: ObjId): Unit = srcObjId = objId
  def get[Value](node: DBNode, attr: Attr[Value]) = {
    val rawAttr = attr.rawAttr
    val key = rawFactConverter.key(node.objId, rawAttr)
    rawFactConverter.valueFromBytes(rawAttr, node.rawIndex.get(key))
  }
  def set[Value](node: DBNode, attr: Attr[Value], value: Value): Unit = {
    val rawAttr = attr.rawAttr
    if (rawAttr.converter.same(get(node, attr),value)) { return }
    val calcList = calcLists.value(attr.nonEmpty.rawAttr)
    if(calcList.isEmpty) throw new Exception(s"$attr is lost")
    for(calc <- calcList) calc.beforeUpdate(node)
    val key = rawFactConverter.key(node.objId, rawAttr)
    val rawValue = rawFactConverter.value(rawAttr, value, srcObjId)
    node.rawIndex.set(key, rawValue)
    for(calc <- calcList) calc.afterUpdate(node)
  }
  def execute(node: DBNode, feed: Feed): Unit = {
    val key = rawFactConverter.keyWithoutAttrId(node.objId)
    node.rawIndex.seek(key)
    rawVisitor.execute(node.rawIndex, key, feed)
  }
}

class AttrCalcLists(components: =>List[ConnectionComponent]) {
  lazy val value: Map[RawAttr[Boolean], List[AttrCalc]] =
    components.collect { case attrCalc: AttrCalc ⇒
      attrCalc.affectedBy.map(attrId => (attrId.nonEmpty.rawAttr, attrCalc))
    }.flatten.groupBy(_._1).mapValues(_.map(_._2))
}
