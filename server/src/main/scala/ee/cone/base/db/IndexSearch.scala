
package ee.cone.base.db

import ee.cone.base.db.Types._

// minKey/merge -> filterRemoved -> takeWhile -> toId

trait KeyPrefixMatcher {
  protected def feed(keyPrefix: RawKey, ks: KeyStatus): Boolean
  protected def execute(tx: RawTx, keyPrefix: RawKey): Unit = {
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
  tx: RawTx
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

class AllOriginalFactExtractor(
  rawFactConverter: RawFactConverter, matcher: RawKeyMatcher, to: Index
) extends KeyPrefixMatcher {
  def from(tx: RawTx) = execute(tx, rawFactConverter.keyHeadOnly)
  protected def feed(keyPrefix: RawKey, ks: KeyStatus): Boolean = {
    if(!matcher.matchPrefix(keyPrefix, ks.key)){ return false }
    val(objId,attrId) = rawFactConverter.keyFromBytes(ks.key)
    to(objId, attrId) = rawFactConverter.valueFromBytes(ks.value) // original
    true
  }
}
