package ee.cone.base.db

import ee.cone.base.util.Never
import ee.cone.base.db.Types._

object Types {
  type RawKey = Array[Byte]
  type RawValue = Array[Byte]
}

class ObjId(val value: Long) extends AnyVal
class AttrId(val value: Long) extends AnyVal

trait AttrInfo
trait Affecting {
  def affects: List[AttrCalc]
}
trait AttrCalc extends AttrInfo {
  def version: String
  def recalculate(objId: ObjId): Unit
  def affectedBy: List[Affecting]
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

// DML/DDL

trait AttrIndex[From,To] {
  def apply(from: From): To
}
trait RuledIndexAdapter[Value] extends AttrIndex[ObjId,Value] {
  def update(objId: ObjId, value: Value): Unit
  def ruled: RuledIndex
}
trait RuledIndex extends AttrIndex[ObjId,DBValue] with Affecting with AttrInfo {
  def update(objId: ObjId, value: DBValue): Unit
  def attrId: AttrId
  def indexed: Boolean
}

trait ValueConverter[A,B] {
  def apply(value: A): B
  def apply(value: B): A
}

trait BaseNameAttrInfo extends AttrInfo {
  def attr: RuledIndex
  def nameOpt: Option[String]
}
trait NameAttrInfo extends BaseNameAttrInfo
trait SearchAttrInfo extends BaseNameAttrInfo {
  def labelAttr: RuledIndex
}
trait SearchAttrInfoFactory {
  def apply(labelOpt: Option[NameAttrInfo], propOpt: Option[NameAttrInfo]): SearchAttrInfo
}

case class ValidationFailure(calc: AttrCalc, objId: ObjId)
trait PreCommitCalcCollector {
  def recalculateAll(): Unit
  def apply(thenDo: Seq[ObjId]=>Seq[ValidationFailure]): ObjId=>Unit
}

// raw converters

trait RawFactConverter {
  def key(objId: ObjId, attrId: AttrId): RawKey
  def keyWithoutAttrId(objId: ObjId): RawKey
  def keyHeadOnly: RawKey
  def value(value: DBValue): RawValue
  def valueFromBytes(b: RawValue): DBValue
  //def keyFromBytes(key: RawKey): (ObjId,AttrId)
}
trait RawSearchConverter {
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
