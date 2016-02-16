
package ee.cone.base.db

import ee.cone.base.db.Types._

// minKey/merge -> filterRemoved -> takeWhile -> toId

class SearchIndexImpl(
  converter: RawSearchConverter,
  val matcher: RawKeyExtractor[ObjId],
  val tx: RawIndex,
  rawVisitor: RawVisitor[ObjId]
) extends SearchIndex {
  def execute(attrId: AttrId, value: DBValue, feed: Feed[ObjId]) = {
    val key = converter.keyWithoutObjId(attrId, value)
    tx.seek(key)
    rawVisitor.execute(key, feed)
  }
  def execute(attrId: AttrId, value: DBValue, objId: ObjId, feed: Feed[ObjId]) = {
    tx.seek(converter.key(attrId, value, objId))
    rawVisitor.execute(converter.keyWithoutObjId(attrId, value), feed)
  }
  def set(attrId: AttrId, value: DBValue, objId: ObjId, on: Boolean): Unit =
    if(value != DBRemoved)
      tx.set(converter.key(attrId, value, objId), converter.value(on))
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