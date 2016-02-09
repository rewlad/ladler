package ee.cone.base.db

import java.nio.ByteBuffer
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

// DML/DDL

trait AttrIndex[From,To] {
  def apply(from: From): To
}
trait UpdatableAttrIndex[Value] extends AttrIndex[ObjId,Value] {
  def update(objId: ObjId, value: Value): Unit
}

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

trait PreCommitCalcCollector {
  def recalculateAll(): Unit
  def apply(thenDo: Seq[ObjId]=>Unit): ObjId=>Unit
}

// raw converters
trait RawValueOuterConverter[Value] {
  def allocWrite(spaceBefore: Int, value: Value, spaceAfter: Int): RawValue
  def read(rawValue: RawValue): Value
  def removed: Value
}
trait RawValueInnerConverter[ValueA,ValueB] {
  def read[Value](rawValue: RawValue, inner: RawValueComposingConverter[ValueA,ValueB,Value]): Value
  def allocWrite(spaceBefore: Int, valueA: ValueA, valueB: ValueB, spaceAfter: Int): RawValue
}
trait RawValueComposingConverter[ValueA,ValueB,Value] {
  def compose(valueA: ValueA, valueB: ValueB): Value
}

trait RawFactConverter[Value] {
  def key(objId: ObjId, attrId: AttrId): RawKey
  def keyWithoutAttrId(objId: ObjId): RawKey
  def keyHeadOnly: RawKey
}
trait RawSearchConverter[Value] {
  def key(attrId: AttrId, value: Value, objId: ObjId): RawKey
  def keyWithoutObjId(attrId: AttrId, value: Value): RawKey
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
