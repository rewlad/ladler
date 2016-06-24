package ee.cone.base.db

import ee.cone.base.connection_api.ObjId

trait RawConverter {
  def toBytes(preId: ObjId, finId: ObjId): Array[Byte]
  def toBytes(preId: ObjId, valHi: Long, valLo: Long, finId: ObjId): Array[Byte]
  def toBytes(preId: ObjId, value: String, finId: ObjId): Array[Byte]
  def fromBytes[Value](b: Array[Byte], skipBefore: Int, converter: RawValueConverter[Value], skipAfter: Int): Value
}

// Value should deal with equal properly for fact update need check
trait RawValueConverter[Value] {
  def convertEmpty(): Value
  def convert(valueA: Long, valueB: Long): Value
  def convert(value: String): Value
  def toBytes(preId: ObjId, value: Value, finId: ObjId): Array[Byte]
}

trait RawDump {
  def apply(b: Array[Byte]): List[Object]
}