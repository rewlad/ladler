package ee.cone.base.db

import ee.cone.base.db.Types._
import ee.cone.base.util.Never

object Types {
  type RawKey = Array[Byte]
  type RawValue = Array[Byte]
}

trait RawIndex {
  def set(key: RawKey, value: RawValue): Unit
  def get(key: RawKey): RawValue
  def seekNext(): Unit
  def seek(from: RawKey): Unit
  def peek: SeekStatus
}

trait SeekStatus {
  def key: RawKey
  def value: RawValue
}

class KeyStatus(val key: RawKey, val value: RawValue) extends SeekStatus

case object NotFoundStatus extends SeekStatus {
  def key = Never()
  def value = Never()
}

trait RawVisitor {
  def execute(tx: RawIndex, whileKeyPrefix: RawKey, feed: RawKey⇒Boolean): Unit
}

trait MuxFactory {
  def wrap(rawIndex: RawIndex): RawIndex
}
