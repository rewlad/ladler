
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

trait ListResult[T] extends KeyPrefixMatcher {
  protected def matcher: RawKeyMatcher
  protected def tx: RawIndex
  protected def lastIdFromLong(id: Long): T

  private var result: List[T] = Nil
  protected def select(key: RawKey): List[T] = {
    result = Nil
    execute(tx, key)
    result.reverse
  }
  protected def feed(keyPrefix: RawKey, ks: KeyStatus): Boolean = {
    if(!matcher.matchPrefix(keyPrefix, ks.key)){ return false }
    result = lastIdFromLong(matcher.lastId(keyPrefix, ks.key)) :: result
    true
  }
}

class ListObjIdsByValueImpl(
  rawIndexConverter: RawIndexConverter,
  val matcher: RawKeyMatcher,
  val tx: RawIndex
) extends ListObjIdsByValue with ListResult[ObjId] {
  def apply(attrId: AttrId, value: DBValue) =
    select(rawIndexConverter.keyWithoutObjId(attrId,value))
  protected def lastIdFromLong(id: Long) = new ObjId(id)
}

class ListAttrIdsByObjIdImpl(
  rawFactConverter: RawFactConverter,
  val matcher: RawKeyMatcher,
  val tx: RawIndex
) extends ListAttrIdsByObjId with ListResult[AttrId] {
  def apply(objId: ObjId) = select(rawFactConverter.keyWithoutAttrId(objId))
  protected def lastIdFromLong(id: Long) = new AttrId(id)
}

class AllFactExtractor(
  rawFactConverter: RawFactConverter, matcher: RawKeyMatcher, to: Index
)(
  whileKeyPrefix: RawKey = rawFactConverter.keyHeadOnly
) extends KeyPrefixMatcher {
  def from(tx: RawIndex) = execute(tx, rawFactConverter.keyHeadOnly)
  def from(tx: RawIndex, objId: ObjId) =
    execute(tx, rawFactConverter.keyWithoutAttrId(objId))
  protected def feed(keyPrefix: RawKey, ks: KeyStatus): Boolean = {
    if(!matcher.matchPrefix(whileKeyPrefix, ks.key)){ return false }
    val(objId,attrId) = ??? //rawFactConverter.keyFromBytes(ks.key)
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