package ee.cone.base.db

import java.util.UUID
import ee.cone.base.util.Never
import ee.cone.base.db.Types._

object Types {
  type RawKey = Array[Byte]
  type RawValue = Array[Byte]
}

class ObjId(val value: Long) extends AnyVal
class AttrId(val value: Long) extends AnyVal

trait AttrInfo
trait AttrCalc extends AttrInfo {
  def version: UUID
  def recalculate(objId: ObjId): Unit
  def affectedByAttrIds: List[AttrId]
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

////////////////////////////////////////////////////////////////////////////////

// DML

trait ListObjIdsByValue {
  def apply(attrId: AttrId, value: DBValue): List[ObjId]
}

trait ListAttrIdsByObjId {
  def apply(objId: ObjId): List[AttrId]
}


trait Index {
  def apply(objId: ObjId, attrId: AttrId): DBValue
  def update(objId: ObjId, attrId: AttrId, value: DBValue): Unit
}

// DDL

trait ValidateFailReaction {
  def apply(objId: ObjId, comment: String): Unit
}

trait IndexAttrInfo { def attrId: AttrId }
trait BaseNameAttrInfo extends AttrInfo {
  def attrId: AttrId
  def nameOpt: Option[String]
}
trait NameAttrInfo extends BaseNameAttrInfo
trait SearchAttrInfo extends BaseNameAttrInfo {
  def labelAttrId: AttrId
}
trait SearchAttrInfoFactory {
  def apply(labelOpt: Option[NameAttrInfo], propOpt: Option[NameAttrInfo]): SearchAttrInfo
}

class SysAttrCalcContext (
  val db: Index,
  val listObjIdsByValue: ListObjIdsByValue,
  val listAttrIdsByObjId: ListAttrIdsByObjId,
  val fail: ValidateFailReaction // fail'll probably do nothing in case of outdated rel type
)

trait PreCommitCalcCollector {
  def recalculateAll(): Unit
  def apply(thenDo: Seq[ObjId]=>Unit): ObjId=>Unit
}
class SysPreCommitCheckContext(
  val db: Index,
  val listObjIdsByValue: ListObjIdsByValue,
  val preCommitCalcCollector: PreCommitCalcCollector,
  val fail: ValidateFailReaction
)

case class AttrUpdate(attrId: AttrId, indexed: Boolean, rewritable: Boolean, calcList: List[AttrCalc])

// raw converters

trait RawFactConverter {
  def key(objId: ObjId, attrId: AttrId): RawKey
  def keyWithoutAttrId(objId: ObjId): RawKey
  def keyHeadOnly: RawKey
  def value(value: DBValue): RawValue
  def valueFromBytes(b: RawValue): DBValue
  //def keyFromBytes(key: RawKey): (ObjId,AttrId)
}
trait RawIndexConverter {
  def key(attrId: AttrId, value: DBValue, objId: ObjId): RawKey
  def keyWithoutObjId(attrId: AttrId, value: DBValue): RawKey
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
