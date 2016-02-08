
package ee.cone.base.db

import ee.cone.base.db.Types._

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

class IndexSearchImpl(
  rawFactConverter: RawFactConverter,
  rawIndexConverter: RawIndexConverter,
  matcher: RawKeyMatcher,
  tx: RawIndex
) extends IndexSearch with KeyPrefixMatcher {
  def apply(objId: Long) = select(rawFactConverter.keyWithoutAttrId(objId))
  def apply(attrId: Long, value: DBValue) =
    select(rawIndexConverter.keyWithoutObjId(attrId,value))
  private var result: List[Long] = Nil
  private def select(key: RawKey): List[Long] = {
    result = Nil
    execute(tx, key)
    result.reverse
  }
  protected def feed(keyPrefix: RawKey, ks: KeyStatus): Boolean = {
    if(!matcher.matchPrefix(keyPrefix, ks.key)){ return false }
    result = matcher.lastId(keyPrefix, ks.key) :: result
    true
  }
}

class AllFactExtractor(
  rawFactConverter: RawFactConverter, matcher: RawKeyMatcher, to: Index
)(
  whileKeyPrefix: RawKey = rawFactConverter.keyHeadOnly
) extends KeyPrefixMatcher {
  def from(tx: RawIndex) = execute(tx, rawFactConverter.keyHeadOnly)
  def from(tx: RawIndex, objId: Long) =
    execute(tx, rawFactConverter.keyWithoutAttrId(objId))
  protected def feed(keyPrefix: RawKey, ks: KeyStatus): Boolean = {
    if(!matcher.matchPrefix(whileKeyPrefix, ks.key)){ return false }
    val(objId,attrId) = rawFactConverter.keyFromBytes(ks.key)
    to(objId, attrId) = rawFactConverter.valueFromBytes(ks.value)
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