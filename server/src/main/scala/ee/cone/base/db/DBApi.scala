package ee.cone.base.db

import java.util.UUID
import ee.cone.base.util.Never
import ee.cone.base.db.LMTypes._

object LMTypes {
  type LMKey = Array[Byte]
  type LMRawValue = Array[Byte]
  type ValueSrcId = Long
}

trait AttrInfo
trait AttrCalc extends AttrInfo {
  def makeACopy: AttrCalc
  def version: UUID
  def apply(objId: Long): Unit
  def affectedByAttrIds: List[Long]
}

trait RawTx {
  def set(key: LMKey, value: LMRawValue): Unit
  def get(key: LMKey): LMRawValue
  def seekNext(): Unit
  def seek(from: LMKey): Unit
  def peek: SeekStatus
}

trait SeekStatus {
  def key: LMKey
  def value: LMRawValue
}
class KeyStatus(val key: LMKey, val value: LMRawValue) extends SeekStatus
case object NotFoundStatus extends SeekStatus {
  def key = Never()
  def value = Never()
}

