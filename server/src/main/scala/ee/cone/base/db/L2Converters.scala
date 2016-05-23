package ee.cone.base.db

import ee.cone.base.connection_api.{CoHandlerProvider, Attr, CoHandler}
import ee.cone.base.db.Types._
import ee.cone.base.util.Never

class DBObjIdValueConverter(
  val valueType: AttrValueType[ObjId],
  inner: RawConverter,
  objIdFactory: ObjIdFactory
) extends RawValueConverter[ObjId] with CoHandlerProvider {
  def convertEmpty() = objIdFactory.noObjId
  def convert(valueA: Long, valueB: Long) = objIdFactory.toObjId(valueA,valueB)
  def convert(value: String) = Never()
  def toBytes(preId: ObjId, value: ObjId, finId: ObjId): RawValue =
    if(value.nonEmpty) inner.toBytes(preId, value.hi, value.lo, finId) else Array()
  def handlers = CoHandler(ToRawValueConverter(valueType))(this) :: Nil
}

class DefinedValueConverter(
  val valueType: AttrValueType[Boolean], inner: RawConverter
) extends RawValueConverter[Boolean] with CoHandlerProvider {
  def convertEmpty() = false
  def convert(valueA: Long, valueB: Long) = true
  def convert(value: String) = true
  def toBytes(preId: ObjId, value: Boolean, finId: ObjId) =
    if(value) Never() else Array()
  def handlers = CoHandler(ToRawValueConverter(valueType))(this) :: Nil
}