package ee.cone.base.db_impl

import java.util.UUID

import ee.cone.base.connection_api._
import ee.cone.base.db.NodeAttrs
import ee.cone.base.db.Types._
import ee.cone.base.util.Never

//import java.lang.Math.toIntExact

abstract class RawValueConverterImpl[IValue] extends RawValueConverter[IValue] with CoHandlerProvider {
  type Value = IValue
  def valueTypes: BasicValueTypes
  def valueType: AttrValueType[Value]
  def toUIString(value: Value): String
  def handlers = List(
    CoHandler(ToRawValueConverter(valueType))(this),
    CoHandler(ConverterKey(valueType,valueTypes.asString))(toUIString)
  )
}

class StringValueConverter(
  val valueTypes: BasicValueTypes, inner: RawConverter
) extends RawValueConverterImpl[String] {
  def valueType = valueTypes.asString
  def convertEmpty() = ""
  def convert(valueA: Long, valueB: Long) = Never()
  def convert(value: String) = value
  def toBytes(preId: ObjId, value: Value, finId: ObjId) =
    if(value.nonEmpty) inner.toBytes(preId, value, finId) else Array()
  def asString = valueType
  def toUIString(value: Value) = value
}

class UUIDValueConverter(
  val valueTypes: BasicValueTypes, inner: RawConverter
) extends RawValueConverterImpl[Option[UUID]] {
  def valueType = valueTypes.asUUID
  def convertEmpty() = None
  def convert(valueA: Long, valueB: Long) = Option(new UUID(valueA,valueB))
  def convert(value: String) = Never()
  def toBytes(preId: ObjId, value: Value, finId: ObjId) =
    if(value.nonEmpty) inner.toBytes(preId, value.get.getMostSignificantBits, value.get.getLeastSignificantBits, finId) else Array()
  def toUIString(value: Value) = "..." //hides keys?
}

class DBObjValueConverter(
  val valueTypes: BasicValueTypes,
  inner: RawValueConverter[ObjId],
  findNodes: FindNodesI,
  nodeAttributes: NodeAttrs
) extends RawValueConverter[Obj] with CoHandlerProvider {
  def valueType = valueTypes.asObj
  def convertEmpty() = findNodes.noNode
  def convert(valueA: Long, valueB: Long) =
    findNodes.whereObjId(inner.convert(valueA,valueB))
  def convert(value: String) = Never()
  def toBytes(preId: ObjId, value: Obj, finId: ObjId) =
    inner.toBytes(preId, value(nodeAttributes.objId), finId)
  def handlers = List(CoHandler(ToRawValueConverter(valueType))(this))
}

class BooleanValueConverter(
  val valueTypes: BasicValueTypes, inner: RawConverter
) extends RawValueConverterImpl[Boolean] {
  def valueType = valueTypes.asBoolean
  def convertEmpty() = false
  def convert(valueA: Long, valueB: Long) = true
  def convert(value: String) = Never()
  def toBytes(preId: ObjId, value: Value, finId: ObjId) =
    if(value) inner.toBytes(preId, 0L, 1L, finId) else Array()
   def toUIString(value: Value) = value.toString
}

class BigDecimalValueConverter(
  val valueTypes: BasicValueTypes, inner: RawConverter
) extends RawValueConverter[Option[BigDecimal]] with CoHandlerProvider {
  def convertEmpty() = None
  private def storageScale = 8
  private def uiScale = 2
  def convert(valueA: Long, valueB: Long) =
    if(valueA == storageScale) Option(BigDecimal(valueB.toLong,storageScale)) else Never()
  def convert(value: String) = Never()
  private def roundingMode = BigDecimal.RoundingMode.HALF_UP
  def toBytes(preId: ObjId, value: Option[BigDecimal], finId: ObjId) =
    if(value.nonEmpty){
      val bgd = value.get.setScale(storageScale, roundingMode)
      val unscaledVal = bgd.bigDecimal.unscaledValue()
      val valueA = storageScale
      val valueB = unscaledVal.longValueExact()
      inner.toBytes(preId,valueA,valueB,finId)
    }
    else Array()

  def toUIString(value: Option[BigDecimal]) =
    value.map(v ⇒v.setScale(uiScale,roundingMode).toString).getOrElse("")
  def fromUIString(value: String):Option[BigDecimal] =
    if(value.nonEmpty) Some(BigDecimal(value)) else None
  def handlers = List(
    CoHandler(ToRawValueConverter(valueTypes.asBigDecimal))(this),
    CoHandler(ConverterKey(valueTypes.asBigDecimal,valueTypes.asString))(toUIString),
    CoHandler(ConverterKey(valueTypes.asString,valueTypes.asBigDecimal))(fromUIString)
  )
}