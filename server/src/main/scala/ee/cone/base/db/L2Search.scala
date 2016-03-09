
package ee.cone.base.db

import ee.cone.base.connection_api.CoHandler
import ee.cone.base.util.Never

// minKey/merge -> filterRemoved -> takeWhile -> toId

class SearchIndexImpl(
  converter: RawSearchConverter,
  rawVisitor: RawVisitor,
  attrFactory: AttrFactory
) extends SearchIndex {
  private def toRawIndex(tx: BoundToTx) =
    if(tx.enabled) tx.asInstanceOf[ProtectedBoundToTx].rawIndex else Never()
  private def execute[Value](attr: RawAttr[Value])(in: SearchRequest[Value]) = {
    val whileKey = converter.keyWithoutObjId(attr, in.value)
    val fromKey = if(in.objId.isEmpty) whileKey
      else converter.key(attr, in.value, in.objId.get)
    val rawIndex = toRawIndex(in.tx)
    rawIndex.seek(fromKey)
    rawVisitor.execute(rawIndex, whileKey, in.feed)
  }
  private def set[Value](attrId: RawAttr[Value], value: Value, node: DBNode, on: Boolean): Unit =
    if(attrId.converter.nonEmpty(value))
      toRawIndex(node.tx).set(converter.key(attrId, value, node.objId), converter.value(on))

  def handlers[Value](labelAttr: Attr[_], propAttr: Attr[Value]) = {
    val labelRawAttr = labelAttr.asInstanceOf[RawAttr[_]]
    val propRawAttr = propAttr.asInstanceOf[RawAttr[Value]]
    if(labelRawAttr.propId!=0L)
      throw new Exception(s"bad index on label: $labelAttr")
    if(propRawAttr.labelId!=0L)
      throw new Exception(s"bad index on prop: $propAttr")
    val attr = attrFactory(labelRawAttr.labelId, propRawAttr.propId, propRawAttr.converter)
    def setter(on: Boolean)(node: DBNode) =
      if (node(labelAttr.defined)) set(attr, node(propAttr), node, on)
    val searchKey = SearchByLabelProp[Value](labelAttr.defined, propAttr.defined)
    CoHandler(searchKey)(execute[Value](attr)) ::
      (labelAttr :: propAttr :: Nil).flatMap{ a =>
        CoHandler(BeforeUpdate(a.defined))(setter(on=false)) ::
        CoHandler(AfterUpdate(a.defined))(setter(on=true)) :: Nil
      }
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