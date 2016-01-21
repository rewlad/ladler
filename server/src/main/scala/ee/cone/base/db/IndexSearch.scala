
package ee.cone.base.db

import ee.cone.base.db.LMTypes._

// minKey/merge -> filterRemoved -> takeWhile -> toId

/*NoGEN*/ trait KeyPrefixMatcher {
  protected def feed(keyPrefix: LMKey, ks: KeyStatus): Boolean
  protected def execute(tx: RawTx, keyPrefix: LMKey): Unit = {
    tx.seek(keyPrefix)
    while(tx.peek match {
      case ks: KeyStatus if feed(keyPrefix, ks) ⇒ tx.seekNext(); true
      case _ ⇒ false
    }) {}
  }
}

/*NoGEN*/ trait IndexSearch {
  def apply(objId: Long): List[Long]
  def apply(attrId: Long, value: LMValue): List[Long]
}

class ImplIndexSearch(tx: RawTx) extends IndexSearch with KeyPrefixMatcher {
  def apply(objId: Long) = select(LMFact.keyWithoutAttrId(objId))
  def apply(attrId: Long, value: LMValue) =
    select(LMIndex.keyWithoutObjId(attrId,value))
  private var result: List[Long] = Nil
  private def select(key: LMKey): List[Long] = {
    result = Nil
    execute(tx, key)
    result.reverse
  }
  protected def feed(keyPrefix: LMKey, ks: KeyStatus): Boolean = {
    if(!LMKey.matchPrefix(keyPrefix, ks.key)){ return false }
    result = LMKey.lastId(keyPrefix, ks.key) :: result
    true
  }
}

class AllOriginalFactExtractor(to: IndexingTx) extends KeyPrefixMatcher {
  private lazy val checkValueSrcId: Option[ValueSrcId⇒Boolean] =
    Some(valueSrcId ⇒ valueSrcId == to.isOriginal)
  def from(tx: RawTx) = execute(tx, LMFact.keyHeadOnly)
  protected def feed(keyPrefix: LMKey, ks: KeyStatus): Boolean = {
    if(!LMKey.matchPrefix(keyPrefix, ks.key)){ return false }
    val value = LMFact.valueFromBytes(ks.value, checkValueSrcId)
    if(value == LMRemoved){ return true }
    val(objId,attrId) = LMFact.keyFromBytes(ks.key)
    to.set(objId, attrId, value, to.isOriginal)
    true
  }
}
