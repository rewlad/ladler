
package ee.cone.base.db

import ee.cone.base.connection_api.CoHandler
import ee.cone.base.util.Never

// minKey/merge -> filterRemoved -> takeWhile -> toId

class SearchIndexImpl(
  converter: RawSearchConverter,
  rawVisitor: RawVisitor,
  attrFactory: AttrFactory
) extends SearchIndex {
  private def execute[Value](attr: Attr[Value])(in: SearchRequest[Value]) = {
    val whileKey = converter.keyWithoutObjId(attr.rawAttr, in.value)
    val fromKey = if(in.objId.isEmpty) whileKey
      else converter.key(attr.rawAttr, in.value, in.objId.get)
    in.tx.rawIndex.seek(fromKey)
    rawVisitor.execute(in.tx.rawIndex, whileKey, in.feed)
  }
  private def set[Value](attrId: RawAttr[Value], value: Value, node: DBNode, on: Boolean): Unit =
    if(attrId.converter.nonEmpty(value))
      node.tx.rawIndex.set(converter.key(attrId, value, node.objId), converter.value(on))

  def handlers[Value](labelAttr: Attr[_], propAttr: Attr[Value]) = {
    if(labelAttr.rawAttr.propId!=0L || propAttr.rawAttr.labelId!=0L) Never()
    val attr = attrFactory(labelAttr.rawAttr.labelId, propAttr.rawAttr.propId, propAttr.rawAttr.converter)
    def setter(on: Boolean)(node: DBNode) =
      if (node(labelAttr.defined)) set(attr.rawAttr, node(propAttr), node, on)
    val searchKey = SearchByLabelProp[Value](labelAttr.defined, propAttr.defined)
    val on = labelAttr.defined :: propAttr.defined :: Nil
    CoHandler(on.map(BeforeUpdate))(setter(on=false)) ::
      CoHandler(on.map(AfterUpdate))(setter(on=true)) ::
      CoHandler(searchKey :: Nil)(execute[Value](attr)) :: Nil
  }
}


/*
case class SearchAttrCalcImpl[Value](searchAttrId: Attr[Value])(
  val on: List[Attr[Boolean]], set: (DBNode,Boolean)=>Unit
) extends SearchAttrCalc[Value] {
  def beforeUpdate(node: DBNode) = set(node, false)
  def handle(node: DBNode) = set(node, true)
}
*/


/*

// was LabelIndexAttrInfoList / LabelPropIndexAttrInfoList
// direct ruled may be composed or labelAttr

class AllFactExtractor(
  rawFactConverter: RawFactConverter, matcher: RawKeyExtractor, to: CalcIndex
)(
  whileKeyPrefix: RawKey = rawFactConverter.keyHeadOnly
) extends KeyPrefixMatcher {
  def from(tx: RawIndex) = execute(tx, rawFactConverter.keyHeadOnly)
  def from(tx: RawIndex, objId: DBNode) =
    execute(tx, rawFactConverter.keyWithoutAttrId(objId))
  protected def feed(keyPrefix: RawKey, ks: KeyStatus): Boolean = {
    if(!matcher.matchPrefix(whileKeyPrefix, ks.key)){ return false }
    val(objId,attrId) = ??? //rawFactConverter.keyFromBytes(ks.key)
    ??? //to(objId, attrId) = rawFactConverter.valueFromBytes(ks.value)
    true
  }
}
*/

/*
class InnerIndexSearch(
  rawFactConverter: RawFactConverter,
  rawIndexConverter: RawIndexConverter,
  matcher: RawKeyMatcher,
  tx: RawTx
) {
  private var selectKey = Array[Byte]()
  private def seek(key: RawKey) = { selectKey = key; tx.seek(key) }
  def seek(): Unit = seek(rawFactConverter.keyHeadOnly)
  def seek(objId: Long): Unit = seek(rawFactConverter.keyWithoutAttrId(objId))
  def seek(attrId: Long, value: DBValue): Unit =
    seek(rawIndexConverter.keyWithoutObjId(attrId,value))
}
*/