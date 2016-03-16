package ee.cone.base.db

import ee.cone.base.db.Types._

class RawVisitorImpl extends RawVisitor {
  def execute(tx: RawIndex, matcher: RawKeyExtractor, keyPrefix: RawKey, minSame: Int, feed: Feed): Unit = {
    while(tx.peek match {
      case ks : KeyStatus if matcher(keyPrefix, minSame, ks.key, feed) ⇒
        tx.seekNext()
        true
      case _ ⇒ false
    }) {}
  }
}
