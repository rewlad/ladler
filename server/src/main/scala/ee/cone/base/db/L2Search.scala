
package ee.cone.base.db

import ee.cone.base.db.Types._
import ee.cone.base.util.Never

// minKey/merge -> filterRemoved -> takeWhile -> toId

class SearchVisitor(
  converter: RawSearchConverter,
  val matcher: RawKeyExtractor[ObjId],
  val tx: RawIndex,
  rawVisitor: RawVisitor[ObjId]
) {
  def execute(attrId: AttrId, value: DBValue, feed: Feed[ObjId]) = {
    val key = converter.keyWithoutObjId(attrId, value)
    tx.seek(key)
    rawVisitor.execute(key, feed)
  }
  def execute(attrId: AttrId, value: DBValue, objId: ObjId, feed: Feed[ObjId]) = {
    tx.seek(converter.key(attrId, value, objId))
    rawVisitor.execute(converter.keyWithoutObjId(attrId, value), feed)
  }
}

class FactVisitor(

){

}



trait ListResult[T] extends KeyPrefixMatcher {
  protected def matcher: RawKeyExtractor
  protected def tx: RawIndex
  protected def lastIdFromLong(keyPrefix: RawKey, key: RawKey): T

  private var result: List[T] = Nil
  protected def select(key: RawKey): List[T] = {
    result = Nil
    execute(tx, key)
    result.reverse
  }
  protected def feed(keyPrefix: RawKey, ks: KeyStatus): Boolean = {
    if(!matcher.matchPrefix(keyPrefix, ks.key)){ return false }
    result = lastIdFromLong(keyPrefix, ks.key) :: result
    true
  }
}

// was LabelIndexAttrInfoList / LabelPropIndexAttrInfoList
// direct ruled may be composed or labelAttr
case class SearchByValueImpl[SearchValue](
  direct: RuledIndexAdapter[SearchValue]
)(
  val ruled: SearchIndexAttrCalc
) extends SearchByValue[SearchValue] {

  def get(value: SearchValue) = ruled.search(direct.converter(value))
}

case class SearchIndexAttrCalc(
  ruled: CalcIndex, version: String = "1"
)(db: SearchIndex) extends AttrCalc {
  def affectedBy: List[Affecting] = ruled :: Nil
  def beforeUpdate(node: DBNode) = db(ruled.attrId, node(ruled), node) = false
  def afterUpdate(node: DBNode) = db(ruled.attrId, node(ruled), node) = true
  def search(value: DBValue) = db(ruled.attrId, value)
}

class SearchIndex(
  val matcher: RawKeyExtractor,
  val tx: RawIndex,
  converter: RawSearchConverter
) extends ListResult[DBNode] {
  def update(attrId: AttrId, value: DBValue, node: DBNode, on: Boolean): Unit =
    if(value != DBRemoved)
      tx.set(converter.key(attrId, value, node.objId), converter.value(on))
  def apply(attrId: AttrId, value: DBValue) =
    select(converter.keyWithoutObjId(attrId, value))
  protected def lastIdFromLong(keyPrefix: RawKey, key: RawKey) =
    new DBNodeImpl(matcher.lastObjId(keyPrefix, key))
}

case class SearchByNodeImpl()(
  rawFactConverter: RawFactConverter,
  val matcher: RawKeyExtractor,
  val tx: RawIndex,
  ruledIndexById: AttrId=>CalcIndex
) extends SearchByNode with ListResult[CalcIndex] {
  def get(node: DBNode) = select(rawFactConverter.keyWithoutAttrId(node.objId))
  protected def lastIdFromLong(keyPrefix: RawKey, key: RawKey) =
    ruledIndexById(matcher.lastAttrId(keyPrefix, key))
}


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