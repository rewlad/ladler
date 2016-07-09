package ee.cone.base.db_impl

import ee.cone.base.db.RawIndex
import ee.cone.base.db.Types._

trait RawVisitor {
  def execute(tx: RawIndex, whileKeyPrefix: RawKey, feed: RawKey⇒Boolean): Unit
}

trait MuxFactory {
  def wrap(rawIndex: RawIndex): RawIndex
}
