package ee.cone.base.db

import ee.cone.base.connection_api.{AttrValueType, CoHandler, CoHandlerProvider, ObjId}
import ee.cone.base.util.Never

class ObjIdSetValueConverter(
  val valueType: AttrValueType[Set[ObjId]], inner: RawConverter, objIdFactory: ObjIdFactory
) extends RawValueConverter[Set[ObjId]] with CoHandlerProvider {
  private def splitter = " "
  def convertEmpty() = Set()
  def convert(valueA: Long, valueB: Long) = Never()
  def convert(value: String) =
    value.split(splitter).map(s⇒objIdFactory.toObjId(s)).toSet
  def toBytes(preId: ObjId, value: Set[ObjId], finId: ObjId) =
    if(value.nonEmpty) inner.toBytes(preId, value.toSeq.map(objIdFactory.toUUIDString).sorted.mkString(splitter), finId) else Array()
  def handlers = List(
    CoHandler(ToRawValueConverter(valueType))(this)
  )
}
