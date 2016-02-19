package ee.cone.base.db

import ee.cone.base.db.Types._

class RawVisitorImpl(matcher: RawKeyExtractor) extends RawVisitor {
  def execute(tx: RawIndex, whileKeyPrefix: RawKey, feed: Feed): Unit = {
    while(tx.peek match {
      case ks: KeyStatus if matcher(whileKeyPrefix, ks.key, feed) ⇒
        tx.seekNext()
        true
      case _ ⇒ false
    }) {}
  }
}
