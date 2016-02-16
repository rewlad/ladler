package ee.cone.base.db

import ee.cone.base.db.Types._

object Types {
  type RawKey = Array[Byte]
  type RawValue = Array[Byte]
  type ObjId = Long
}

case class AttrId(labelId: Long, propId: Long)

trait Feed[T]{
  def apply(value: T): Boolean
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
trait RawKeyExtractor[T] {
  def apply(keyPrefix: RawKey, key: RawKey, feed: Feed[T]): Boolean
}

// DBValue

sealed abstract class DBValue
case object DBRemoved extends DBValue
case class DBStringValue(value: String) extends DBValue
case class DBLongValue(value: Long) extends DBValue
case class DBLongPairValue(valueA: Long, valueB: Long) extends DBValue

