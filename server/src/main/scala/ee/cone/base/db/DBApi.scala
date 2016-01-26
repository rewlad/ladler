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
  def version: UUID
  def recalculate(objId: Long): Unit
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

////////////////////////////////////////////////////////////////////////////////

// DML

trait IndexSearch {
  def apply(objId: Long): List[Long]
  def apply(attrId: Long, value: LMValue): List[Long]
}

trait IndexingTx {
  def apply(objId: Long, attrId: Long): LMValue
  def update(objId: Long, attrId: Long, value: LMValue): Unit
  def isOriginal = 1L
  def set(objId: Long, attrId: Long, value: LMValue, valueSrcId: ValueSrcId): Boolean
}

// DDL

trait ValidateFailReaction {
  def apply(objId: Long, comment: String): Unit
}

trait IndexAttrInfo { def attrId: Long }
trait BaseNameAttrInfo extends AttrInfo {
  def attrId: Long
  def nameOpt: Option[String]
}
trait NameAttrInfo extends BaseNameAttrInfo
trait SearchAttrInfo extends BaseNameAttrInfo {
  def labelAttrId: Long
}
trait SearchAttrInfoFactory {
  def apply(labelOpt: Option[NameAttrInfo], propOpt: Option[NameAttrInfo]): SearchAttrInfo
}

class SysAttrCalcContext (
  val db: IndexingTx,
  val indexSearch: IndexSearch,
  val fail: ValidateFailReaction // fail'll probably do nothing in case of outdated rel type
)

trait PreCommitCalcCollector {
  def recalculateAll(): Unit
  def apply(thenDo: Seq[Long]=>Unit): Long=>Unit
}
class SysPreCommitCheckContext(
  val db: IndexingTx,
  val indexSearch: IndexSearch,
  val preCommitCalcCollector: PreCommitCalcCollector,
  val fail: ValidateFailReaction
)

// raw converters

trait RawFactConverter {
  def key(objId: Long, attrId: Long): LMKey
  def keyWithoutAttrId(objId: Long): LMKey
  def keyHeadOnly: LMKey
  def value(value: LMValue, valueSrcId: ValueSrcId): LMRawValue
  def valueFromBytes(b: LMRawValue, check: Option[ValueSrcId⇒Boolean]): LMValue
  def keyFromBytes(key: LMKey): (Long,Long)
}
trait RawIndexConverter {
  def key(attrId: Long, value: LMValue, objId: Long): LMKey
  def keyWithoutObjId(attrId: Long, value: LMValue): LMKey
  def value(on: Boolean): LMRawValue
}
trait RawKeyMatcher {
  def matchPrefix(keyPrefix: LMKey, key: LMKey): Boolean
  def lastId(keyPrefix: LMKey, key: LMKey): Long
}

// LMValue

sealed abstract class LMValue
case object LMRemoved extends LMValue
case class LMStringValue(value: String) extends LMValue
case class LMLongValue(value: Long) extends LMValue
case class LMLongPairValue(valueA: Long, valueB: Long) extends LMValue
