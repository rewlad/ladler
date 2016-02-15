package ee.cone.base.db

import ee.cone.base.util.Never
import ee.cone.base.db.Types._

object Types {
  type RawKey = Array[Byte]
  type RawValue = Array[Byte]
  type ObjId = Long
}

case class AttrId(labelId: Long, propId: Long)

trait AttrInfo
trait Affecting {
  def affects(calc: AttrCalc): Unit
}
trait Affected {
  def version: String
  def affectedBy: List[Affecting]
}
trait AttrCalc extends AttrInfo with Affected {
  def beforeUpdate(node: DBNode): Unit
  def afterUpdate(node: DBNode): Unit
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

trait DBNode {
  def objId: Long
  def apply[Value](attr: AttrIndex[DBNode,Value]): Value
  def update[Value](attr: UpdatableAttrIndex[Value], value: Value): Unit
}


class DBNodeImpl(val objId: Long) extends DBNode {
  def apply[Value](attr: AttrIndex[DBNode,Value]) = attr.get(this)
  def update[Value](attr: UpdatableAttrIndex[Value], value: Value) =
    attr.set(this,value)
}

trait AttrIndex[From,To] {
  def get(from: From): To
}
trait DBValueConverter[A] {
  def apply(value: A): DBValue
  def apply(value: DBValue): A
}
trait UpdatableAttrIndex[Value] extends AttrIndex[DBNode,Value] {
  def set(node: DBNode, value: Value): Unit
}
trait CalcIndex extends UpdatableAttrIndex[DBValue] with Affecting with AttrInfo {
  def attrId: AttrId
}
trait RuledIndexAdapter[Value] extends UpdatableAttrIndex[Value] {
  def ruled: CalcIndex
  def converter: DBValueConverter[Value]
}
trait SearchByValue[Value] extends AttrIndex[Value,List[DBNode]] {
  def direct: RuledIndexAdapter[Value]
}
trait SearchByNode extends AttrIndex[DBNode,List[CalcIndex]]

trait IndexComposer {
  def apply(labelAttr: CalcIndex, propAttr: CalcIndex): CalcIndex
}

case class ValidationFailure(calc: PreCommitCheck, node: DBNode)
trait PreCommitCheckAttrCalc extends AttrCalc {
  def checkAll(): Seq[ValidationFailure]
}
trait PreCommitCheck extends Affected {
  def check(nodes: Seq[DBNode]): Seq[ValidationFailure]
}

// raw converters

trait RawFactConverter {
  def key(objId: ObjId, attrId: AttrId): RawKey
  def keyWithoutAttrId(objId: ObjId): RawKey
  def keyHeadOnly: RawKey
  def value(value: DBValue, valueSrcId: ObjId): RawValue
  def valueFromBytes(b: RawValue): DBValue
  //def keyFromBytes(key: RawKey): (ObjId,AttrId)
}
trait RawSearchConverter {
  def key(attrId: AttrId, value: DBValue, objId: ObjId): RawKey
  def keyWithoutObjId(attrId: AttrId, value: DBValue): RawKey
  def value(on: Boolean): RawValue
}
trait Feed[T]{
  def apply(value: T): Boolean
}
trait RawKeyExtractor[T] {
  def apply(keyPrefix: RawKey, key: RawKey, feed: Feed[T]): Boolean
}
trait RawVisitor[T] {
  def execute(whileKeyPrefix: RawKey, feed: Feed[T]): Unit
}

// DBValue

sealed abstract class DBValue
case object DBRemoved extends DBValue
case class DBStringValue(value: String) extends DBValue
case class DBLongValue(value: Long) extends DBValue
case class DBLongPairValue(valueA: Long, valueB: Long) extends DBValue
