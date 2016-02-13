
package ee.cone.base.db

import ee.cone.base.db.Types._
import ee.cone.base.util.Never

// minKey/merge -> filterRemoved -> takeWhile -> toId

trait KeyPrefixMatcher {
  protected def feed(keyPrefix: RawKey, ks: KeyStatus): Boolean
  protected def execute(tx: RawIndex, keyPrefix: RawKey): Unit = {
    tx.seek(keyPrefix)
    while(tx.peek match {
      case ks: KeyStatus if feed(keyPrefix, ks) ⇒ tx.seekNext(); true
      case _ ⇒ false
    }) {}
  }
}

trait ListResult[T] extends KeyPrefixMatcher {
  protected def matcher: RawKeyMatcher
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
  direct: RuledIndexAdapter[SearchValue], version: String = "1"
)(
  converter: RawSearchConverter,
  val matcher: RawKeyMatcher,
  val tx: RawIndex
) extends AttrCalc with SearchByValue[SearchValue] with ListResult[ObjId] {
  def affectedBy: List[Affecting] = direct.ruled :: Nil
  def beforeUpdate(objId: ObjId): Unit = update(objId, direct.ruled(objId), on=false)
  def afterUpdate(objId: ObjId): Unit = update(objId, direct.ruled(objId), on=true)
  private def update(objId: ObjId, value: DBValue, on: Boolean): Unit =
    if(value != DBRemoved)
      tx.set(converter.key(direct.ruled.attrId, value, objId), converter.value(on))
  def apply(value: SearchValue) =
    select(converter.keyWithoutObjId(direct.ruled.attrId, direct.converter(value)))
  protected def lastIdFromLong(keyPrefix: RawKey, key: RawKey) =
    matcher.lastObjId(keyPrefix, key)
}

case class SearchByObjIdImpl()(
  rawFactConverter: RawFactConverter,
  val matcher: RawKeyMatcher,
  val tx: RawIndex,
  ruledIndexById: AttrId=>RuledIndex
) extends SearchByObjId with ListResult[RuledIndex] {
  def apply(objId: ObjId) = select(rawFactConverter.keyWithoutAttrId(objId))
  protected def lastIdFromLong(keyPrefix: RawKey, key: RawKey) =
    ruledIndexById(matcher.lastAttrId(keyPrefix, key))
}

class AllFactExtractor(
  rawFactConverter: RawFactConverter, matcher: RawKeyMatcher, to: RuledIndex
)(
  whileKeyPrefix: RawKey = rawFactConverter.keyHeadOnly
) extends KeyPrefixMatcher {
  def from(tx: RawIndex) = execute(tx, rawFactConverter.keyHeadOnly)
  def from(tx: RawIndex, objId: ObjId) =
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