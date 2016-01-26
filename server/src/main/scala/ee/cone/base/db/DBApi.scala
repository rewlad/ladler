package ee.cone.base.db

import java.util.UUID
import ee.cone.base.util.Never
import ee.cone.base.db.Types._

object Types {
  type RawKey = Array[Byte]
  type RawValue = Array[Byte]
  type ValueSrcId = Long
}

trait AttrInfo
trait AttrCalc extends AttrInfo {
  def version: UUID
  def recalculate(objId: Long): Unit
  def affectedByAttrIds: List[Long]
}

trait RawTx {
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

////////////////////////////////////////////////////////////////////////////////

// DML

trait IndexSearch {
  def apply(objId: Long): List[Long]
  def apply(attrId: Long, value: DBValue): List[Long]
}

trait IndexingTx {
  def apply(objId: Long, attrId: Long): DBValue
  def update(objId: Long, attrId: Long, value: DBValue): Unit
  def isOriginal = 1L
  def set(objId: Long, attrId: Long, value: DBValue, valueSrcId: ValueSrcId): Boolean
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
  def key(objId: Long, attrId: Long): RawKey
  def keyWithoutAttrId(objId: Long): RawKey
  def keyHeadOnly: RawKey
  def value(value: DBValue, valueSrcId: ValueSrcId): RawValue
  def valueFromBytes(b: RawValue, check: Option[ValueSrcId⇒Boolean]): DBValue
  def keyFromBytes(key: RawKey): (Long,Long)
}
trait RawIndexConverter {
  def key(attrId: Long, value: DBValue, objId: Long): RawKey
  def keyWithoutObjId(attrId: Long, value: DBValue): RawKey
  def value(on: Boolean): RawValue
}
trait RawKeyMatcher {
  def matchPrefix(keyPrefix: RawKey, key: RawKey): Boolean
  def lastId(keyPrefix: RawKey, key: RawKey): Long
}

// DBValue

sealed abstract class DBValue
case object DBRemoved extends DBValue
case class DBStringValue(value: String) extends DBValue
case class DBLongValue(value: Long) extends DBValue
case class DBLongPairValue(valueA: Long, valueB: Long) extends DBValue
