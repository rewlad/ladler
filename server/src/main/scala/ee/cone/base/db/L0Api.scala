package ee.cone.base.db

import ee.cone.base.db.Types._

object Types {
  type RawKey = Array[Byte]
  type RawValue = Array[Byte]
  type ObjId = Long
}

trait RawAttr[Value] {
  def labelId: Long
  def propId: Long
  def converter: RawValueConverter[Value]
}

// raw converters

trait RawFactConverter {
  def key(objId: ObjId, attrId: RawAttr[_]): RawKey
  def keyWithoutAttrId(objId: ObjId): RawKey
  def keyHeadOnly: RawKey
  def value[Value](attrId: RawAttr[Value], value: Value, valueSrcId: ObjId): RawValue
  def valueFromBytes[Value](converter: RawValueConverter[Value], b: RawValue): Value
  //def keyFromBytes(key: RawKey): (ObjId,AttrId)
}
trait RawSearchConverter {
  def key[Value](attrId: RawAttr[Value], value: Value, objId: ObjId): RawKey
  def keyWithoutObjId[Value](attrId: RawAttr[Value], value: Value): RawKey
  def value(on: Boolean): RawValue
}
trait RawKeyExtractor {
  def apply(keyPrefix: RawKey, minSame: Int, key: RawKey, feed: Feed): Boolean
}
trait Feed {
  def apply(valueA: Long, valueB: Long): Boolean
}

// Value should deal with equal properly for fact update need check
trait RawValueConverter[Value] {
  def convertEmpty(): Value
  def convert(valueA: Long, valueB: Long): Value
  def convert(value: String): Value
  def nonEmpty(value: Value): Boolean
  def allocWrite(before: Int, value: Value, after: Int): RawValue
}
trait InnerRawValueConverter {
  def allocWrite(spaceBefore: Int, valueA: Long, valueB: Long, spaceAfter: Int): RawValue
  def allocWrite(spaceBefore: Int, value: String, spaceAfter: Int): RawValue
}
