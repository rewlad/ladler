
package ee.cone.base.db

import ee.cone.base.db.Types._
import ee.cone.base.util.Never

// minKey/merge -> filterRemoved -> takeWhile -> toId

class SearchIndexImpl(
  converter: RawSearchConverter,
  val matcher: RawKeyExtractor[ObjId],
  val tx: RawIndex,
  rawVisitor: RawVisitor[ObjId],
  direct: FactIndex,
  hasAttrCalc: AttrId => Boolean
) extends SearchIndex {
  def execute(attrId: AttrId, value: DBValue, feed: Feed[ObjId]) = {
    if(!hasAttrCalc(attrId)) Never()
    val key = converter.keyWithoutObjId(attrId, value)
    tx.seek(key)
    rawVisitor.execute(key, feed)
  }
  def execute(attrId: AttrId, value: DBValue, objId: ObjId, feed: Feed[ObjId]) = {
    if(!hasAttrCalc(attrId)) Never()
    tx.seek(converter.key(attrId, value, objId))
    rawVisitor.execute(converter.keyWithoutObjId(attrId, value), feed)
  }
  private def set(attrId: AttrId, value: DBValue, objId: ObjId, on: Boolean): Unit =
    if(value != DBRemoved)
      tx.set(converter.key(attrId, value, objId), converter.value(on))

  def composeAttrId(labelAttrId: AttrId, propAttrId: AttrId): AttrId = {
    val AttrId(labelAttrIdPart,0) = labelAttrId
    val AttrId(0,propAttrIdPart) = propAttrId
    new AttrId(labelAttrIdPart,propAttrIdPart)
  }
  def attrCalc(attrId: AttrId) = attrId match {
    case AttrId(_,0) | AttrId(0,_) =>
      new SearchAttrCalcImpl(attrId)(attrId :: Nil, (objId,on)=>{
        set(attrId, direct.get(objId, attrId), objId, on)
      })
    case AttrId(labelAttrIdPart,propAttrIdPart) =>
      val labelAttrId = new AttrId(labelAttrIdPart,0)
      val propAttrId = new AttrId(0, propAttrIdPart)
      new SearchAttrCalcImpl(attrId)(labelAttrId :: propAttrId :: Nil, (objId,on)=>{
        val value = if(direct.get(objId, labelAttrId)==DBRemoved) DBRemoved
        else direct.get(objId, propAttrId)
        set(attrId, value, objId, on)
      })
  }
}

case class SearchAttrCalcImpl(searchAttrId: AttrId)(
  val affectedBy: List[AttrId], set: (ObjId,Boolean)=>Unit
) extends SearchAttrCalc {
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