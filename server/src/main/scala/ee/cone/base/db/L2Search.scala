
package ee.cone.base.db

import ee.cone.base.connection_api.{ConnectionComponent, Registration}
import ee.cone.base.db.Types._
import ee.cone.base.util.Never

// minKey/merge -> filterRemoved -> takeWhile -> toId

case class AttrIdImpl[Value](labelId: Long, propId: Long)
  (val converter: RawValueConverter[Value], val booleanConverter: RawValueConverter[Boolean])
  (val nonEmpty: AttrId[Boolean] = new BooleanAttrId(labelId,propId)(booleanConverter))
  extends AttrId[Value]
case class BooleanAttrId(labelId: Long, propId: Long)(val converter: RawValueConverter[Boolean]) extends AttrId[Boolean] {
  def nonEmpty = this
}

class SearchRawIndexRegistration(index: SearchIndexImpl, tx: RawIndex) extends Registration {
  def open() = index.txOpt = Option(tx)
  def close() = index.txOpt = None
}

class SearchAttrCalcCheck(components: =>List[ConnectionComponent]) {
  private lazy val value =
    components.collect { case a: SearchAttrCalc[_] ⇒ a.searchAttrId.nonEmpty }.toSet
  def apply(attrId: AttrId[Boolean]) =
    if(!value(attrId)) throw new Exception(s"$attrId is lost")
}

class SearchIndexImpl(
  converter: RawSearchConverter,
  rawVisitor: RawVisitor,
  direct: FactIndex,
  check: SearchAttrCalcCheck
) extends SearchIndex {
  var txOpt: Option[RawIndex] = None
  def tx = txOpt.get

  def execute[Value](attrId: AttrId[Value], value: Value, feed: Feed) = {
    check(attrId.nonEmpty)
    val key = converter.keyWithoutObjId(attrId, value)
    tx.seek(key)
    rawVisitor.execute(tx, key, feed)
  }
  def execute[Value](attrId: AttrId[Value], value: Value, objId: ObjId, feed: Feed) = {
    check(attrId.nonEmpty)
    tx.seek(converter.key(attrId, value, objId))
    rawVisitor.execute(tx, converter.keyWithoutObjId(attrId, value), feed)
  }
  private def set[Value](attrId: AttrId[Value], value: Value, objId: ObjId, on: Boolean): Unit =
    if(attrId.converter.nonEmpty(value))
      tx.set(converter.key(attrId, value, objId), converter.value(on))

  def composeAttrId[Value](labelAttrId: AttrId[Boolean], propAttrId: AttrId[Value]): AttrId[Value] =
    if(labelAttrId.propId==0L && propAttrId.labelId==0L)
      new AttrIdImpl[Value](labelAttrId.labelId,propAttrId.propId)(propAttrId.converter,propAttrId.nonEmpty.converter)() else Never()

  def attrCalc[Value](attrId: AttrId[Value]) =
    if(attrId.labelId==0 || attrId.propId==0)
      new SearchAttrCalcImpl(attrId)(attrId.nonEmpty :: Nil, (objId,on)=>{
        set(attrId, direct.get(objId, attrId), objId, on)
      })
    else {
      val labelAttrId = new BooleanAttrId(attrId.labelId, 0)(attrId.nonEmpty.converter)
      val propAttrId = new AttrIdImpl[Value](0, attrId.propId)(attrId.converter,attrId.nonEmpty.converter)()
      new SearchAttrCalcImpl(attrId)(labelAttrId.nonEmpty :: propAttrId.nonEmpty :: Nil, (objId, on) =>
        if (direct.get(objId, labelAttrId))
          set(attrId, direct.get(objId, propAttrId), objId, on)
      )
    }

}

case class SearchAttrCalcImpl[Value](searchAttrId: AttrId[Value])(
  val affectedBy: List[AttrId[Boolean]], set: (ObjId,Boolean)=>Unit
) extends SearchAttrCalc[Value] {
  def beforeUpdate(objId: ObjId) = set(objId, false)
  def afterUpdate(objId: ObjId) = set(objId, true)
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