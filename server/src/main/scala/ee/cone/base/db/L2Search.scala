
package ee.cone.base.db

import ee.cone.base.connection_api.ConnectionComponent
import ee.cone.base.db.Types._
import ee.cone.base.util.Never

// minKey/merge -> filterRemoved -> takeWhile -> toId

class SearchAttrCalcCheck(components: =>List[ConnectionComponent]) {
  private lazy val ids: Set[Attr[Boolean]] = components.collect{
    case handler: SearchAttr[_] => handler.attr.nonEmpty
  }.toSet
  def apply(attr: Attr[_]) =
    if(!ids(attr.nonEmpty)) throw new Exception(s"$attr is lost")
}

class SearchIndexImpl(
  converter: RawSearchConverter,
  rawVisitor: RawVisitor,
  attrFactory: AttrFactory,
  check: SearchAttrCalcCheck
) extends SearchIndex {
  def execute[Value](tx: RawTx, attr: Attr[Value], value: Value, feed: Feed) = {
    check(attr)
    val key = converter.keyWithoutObjId(attr.rawAttr, value)
    tx.rawIndex.seek(key)
    rawVisitor.execute(tx.rawIndex, key, feed)
  }
  def execute[Value](tx: RawTx, attr: Attr[Value], value: Value, objId: ObjId, feed: Feed) = {
    check(attr)
    tx.rawIndex.seek(converter.key(attr.rawAttr, value, objId))
    rawVisitor.execute(tx.rawIndex, converter.keyWithoutObjId(attr.rawAttr, value), feed)
  }
  private def set[Value](attrId: RawAttr[Value], value: Value, node: DBNode, on: Boolean): Unit =
    if(attrId.converter.nonEmpty(value))
      node.tx.rawIndex.set(converter.key(attrId, value, node.objId), converter.value(on))

  private def calcPair[Value](
    attr: Attr[Value], on: List[Attr[Boolean]], setter: Boolean=>DBNode=>Unit
  ): List[ConnectionComponent] =
    new CoHandlerImpl(on.map(BeforeUpdate), setter(false)) ::
    new CoHandlerImpl(on.map(AfterUpdate), setter(true)) ::
    SearchAttr(attr) :: Nil
  def attrCalc[Value](attr: Attr[Value]) = {
    if(attr.rawAttr.propId!=0L && attr.rawAttr.labelId!=0L) Never()
    def setter(on: Boolean)(node: DBNode) = set(attr.rawAttr, node(attr), node, on)
    calcPair(attr, attr.nonEmpty :: Nil, setter)
  }
  def attrCalc[Value](labelAttr: Attr[Boolean], propAttr: Attr[Value]) = {
    if(labelAttr.rawAttr.propId!=0L || propAttr.rawAttr.labelId!=0L) Never()
    val attr = attrFactory(labelAttr.rawAttr.labelId, propAttr.rawAttr.propId, propAttr.rawAttr.converter)
    def setter(on: Boolean)(node: DBNode) =
      if (node(labelAttr)) set(attr.rawAttr, node(propAttr), node, on)
    (attr, calcPair(attr, labelAttr.nonEmpty :: propAttr.nonEmpty :: Nil, setter))
  }
}

class CoHandlerImpl[In,Out](val on: List[EventKey[In,Out]], doHandle: In=>Out) extends CoHandler[In,Out] {
  def handle(value: In) = doHandle(value)
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