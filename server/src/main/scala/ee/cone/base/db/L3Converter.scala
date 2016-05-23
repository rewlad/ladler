package ee.cone.base.db

import java.util.UUID

import ee.cone.base.connection_api.{CoHandlerProvider, CoHandler, Obj, Attr}
import ee.cone.base.db.Types._
import ee.cone.base.util.Never

//import java.lang.Math.toIntExact

abstract class RawValueConverterImpl[IValue] extends RawValueConverter[IValue] with CoHandlerProvider {
  type Value = IValue
  def valueType: AttrValueType[Value]
  def handlers = CoHandler(ToRawValueConverter(valueType))(this) :: Nil
}

class StringValueConverter(
  val valueType: AttrValueType[String], inner: RawConverter
) extends RawValueConverterImpl[String] {
  def convertEmpty() = ""
  def convert(valueA: Long, valueB: Long) = Never()
  def convert(value: String) = value
  def toBytes(preId: ObjId, value: Value, finId: ObjId) =
    if(value.nonEmpty) inner.toBytes(preId, value, finId) else Array()
}

class UUIDValueConverter(
  val valueType: AttrValueType[Option[UUID]], inner: RawConverter
) extends RawValueConverterImpl[Option[UUID]] {
  def convertEmpty() = None
  def convert(valueA: Long, valueB: Long) = Option(new UUID(valueA,valueB))
  def convert(value: String) = Never()
  def toBytes(preId: ObjId, value: Value, finId: ObjId) =
    if(value.nonEmpty) inner.toBytes(preId, value.get.getMostSignificantBits, value.get.getLeastSignificantBits, finId) else Array()
}

class DBObjValueConverter(
  val valueType: AttrValueType[Obj],
  inner: DBObjIdValueConverter,
  findNodes: FindNodes,
  nodeAttributes: NodeAttrs
) extends RawValueConverterImpl[Obj] {
  def convertEmpty() = findNodes.noNode
  def convert(valueA: Long, valueB: Long) =
    findNodes.whereObjId(inner.convert(valueA,valueB))
  def convert(value: String) = Never()
  def toBytes(preId: ObjId, value: Value, finId: ObjId) =
    inner.toBytes(preId, value(nodeAttributes.objId), finId)
}

class BooleanValueConverter(
  val valueType: AttrValueType[Boolean], inner: RawConverter
) extends RawValueConverterImpl[Boolean] {
  def convertEmpty() = false
  def convert(valueA: Long, valueB: Long) = true
  def convert(value: String) = Never()
  def toBytes(preId: ObjId, value: Value, finId: ObjId) =
    if(value) inner.toBytes(preId, 0L, 1L, finId) else Array()
}
