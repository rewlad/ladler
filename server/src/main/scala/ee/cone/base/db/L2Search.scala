
package ee.cone.base.db

import ee.cone.base.connection_api.{ConnectionComponent, Registration}
import ee.cone.base.db.Types._
import ee.cone.base.util.Never

// minKey/merge -> filterRemoved -> takeWhile -> toId

class SearchRawIndexRegistration(index: SearchIndexImpl, tx: RawIndex) extends Registration {
  def open() = index.txOpt = Option(tx)
  def close() = index.txOpt = None
}

class SearchAttrCalcCheck(components: =>List[ConnectionComponent]) {
  private lazy val value =
    components.collect { case a: SearchAttrCalc[_] ⇒ a.searchAttrId.nonEmpty.rawAttr }.toSet
  def apply(attrId: Attr[Boolean]) =
    if(!value(attrId.rawAttr)) throw new Exception(s"$attrId is lost")
}

class SearchIndexImpl(
  converter: RawSearchConverter,
  rawVisitor: RawVisitor,
  attrFactory: AttrFactory,
  check: SearchAttrCalcCheck
) extends SearchIndex {
  var txOpt: Option[RawIndex] = None
  def tx = txOpt.get

  def execute[Value](attr: Attr[Value], value: Value, feed: Feed) = {
    check(attr.nonEmpty)
    val key = converter.keyWithoutObjId(attr.rawAttr, value)
    tx.seek(key)
    rawVisitor.execute(tx, key, feed)
  }
  def execute[Value](attr: Attr[Value], value: Value, objId: ObjId, feed: Feed) = {
    check(attr.nonEmpty)
    tx.seek(converter.key(attr.rawAttr, value, objId))
    rawVisitor.execute(tx, converter.keyWithoutObjId(attr.rawAttr, value), feed)
  }
  private def set[Value](attrId: RawAttr[Value], value: Value, node: DBNode, on: Boolean): Unit =
    if(attrId.converter.nonEmpty(value))
      node.rawIndex.set(converter.key(attrId, value, node.objId), converter.value(on))

  def attrCalc[Value](attr: Attr[Value]) = {
    if(attr.rawAttr.propId!=0L && attr.rawAttr.labelId!=0L) Never()
    new SearchAttrCalcImpl(attr)(attr.nonEmpty :: Nil, (node,on)=>{
      set(attr.rawAttr, node(attr), node, on)
    })
  }
  def attrCalc[Value](labelAttr: Attr[Boolean], propAttr: Attr[Value]) = {
    if(labelAttr.rawAttr.propId!=0L || propAttr.rawAttr.labelId!=0L) Never()
    val attr = attrFactory(labelAttr.rawAttr.labelId, propAttr.rawAttr.propId, propAttr.rawAttr.converter)
    new SearchAttrCalcImpl(attr)(labelAttr.nonEmpty :: propAttr.nonEmpty :: Nil, (node, on) =>
      if (node(labelAttr)) set(attr.rawAttr, node(propAttr), node, on)
    )
  }
}

case class SearchAttrCalcImpl[Value](searchAttrId: Attr[Value])(
  val affectedBy: List[Attr[Boolean]], set: (DBNode,Boolean)=>Unit
) extends SearchAttrCalc[Value] {
  def beforeUpdate(node: DBNode) = set(node, false)
  def afterUpdate(node: DBNode) = set(node, true)
}



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