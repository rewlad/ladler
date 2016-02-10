package ee.cone.base.db

import ee.cone.base.util.Never
import ee.cone.base.db.Types._

object Types {
  type RawKey = Array[Byte]
  type RawValue = Array[Byte]
}

class ObjId(val value: Long) extends AnyVal
case class AttrId(labelId: Long, propId: Long)

trait AttrInfo
trait Affecting {
  def affects: List[Affected]
}
trait Affected {
  def version: String
  def affectedBy: List[Affecting]
}
trait AttrCalc extends AttrInfo with Affected {
  def recalculate(objId: ObjId): Unit
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
trait ValueConverter[A,B] {
  def apply(value: A): B
  def apply(value: B): A
}
trait RuledIndex extends AttrIndex[ObjId,DBValue] with Affecting with AttrInfo {
  def update(objId: ObjId, value: DBValue): Unit
  def attrId: AttrId
  def indexed: Boolean
}
trait RuledIndexAdapter[Value] extends AttrIndex[ObjId,Value] {
  def update(objId: ObjId, value: Value): Unit
  def ruled: RuledIndex
  def converter: ValueConverter[Value,DBValue]
}
trait SearchByValue[Value] extends AttrIndex[Value,List[ObjId]] {
  def direct: RuledIndexAdapter[Value]
}
trait SearchByObjId extends AttrIndex[ObjId,List[RuledIndex]]

trait IndexComposer {
  def apply(labelAttr: RuledIndex, propAttr: RuledIndex): RuledIndex
}

case class ValidationFailure(calc: PreCommitCheck, objId: ObjId)
trait PreCommitCheckAttrCalc extends AttrCalc {
  def checkAll(): Seq[ValidationFailure]
}
trait PreCommitCheck extends Affected {
  def check(objIds: Seq[ObjId]): Seq[ValidationFailure]
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
  def lastObjId(keyPrefix: RawKey, key: RawKey): ObjId
  def lastAttrId(keyPrefix: RawKey, key: RawKey): AttrId
}

// DBValue

sealed abstract class DBValue
case object DBRemoved extends DBValue
case class DBStringValue(value: String) extends DBValue
case class DBLongValue(value: Long) extends DBValue
case class DBLongPairValue(valueA: Long, valueB: Long) extends DBValue
