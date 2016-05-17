package ee.cone.base.db

import ee.cone.base.connection_api.{CoHandlerProvider, Attr, CoHandler}
import ee.cone.base.db.Types._
import ee.cone.base.util.Never

class AttrValueConverter(
  val valueType: AttrValueType[Attr[Boolean]],
  inner: RawConverter,
  attrFactory: AttrFactory, asDefined: AttrValueType[Boolean]
) extends RawValueConverter[Attr[Boolean]] with CoHandlerProvider {
  def convertEmpty() = attrFactory.noAttr
  def convert(valueA: Long, valueB: Long) =
    attrFactory(valueA,valueB,asDefined)
  def convert(value: String) = Never()
  def toBytes(preId: ObjId, value: Attr[Boolean], finId: ObjId) = {
    val attr = value.asInstanceOf[ObjId]
    if(attr.nonEmpty) inner.toBytes(preId, attr.hi, attr.lo, finId) else Array()
  }
  def handlers = CoHandler(ToRawValueConverter(valueType))(this) :: Nil
}

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
