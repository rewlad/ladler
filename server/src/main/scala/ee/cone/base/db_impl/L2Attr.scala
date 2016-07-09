package ee.cone.base.db_impl

import java.nio.ByteBuffer
import java.util.UUID

import ee.cone.base.connection_api._
import ee.cone.base.util.Never

class AttrFactoryImpl(
  handlerLists: CoHandlerLists,
  objIdFactory: ObjIdFactoryI,
  dbWrapType: WrapType[ObjId]
) extends AttrFactoryI {
  def apply[V](uuid: String, valueType: AttrValueType[V]): Attr[V] =
    define(objIdFactory.toObjId(uuid), valueType)
  def define[V](attrId: ObjId, valueType: AttrValueType[V]): Attr[V] =
    new AttrImpl(attrId, valueType)
  def attrId[V](attr: Attr[V]): ObjId = attr.asInstanceOf[AttrImpl[V]].id
  def valueType[V](attr: Attr[V]): AttrValueType[V] = attr.asInstanceOf[AttrImpl[V]].valueType
  def toAttr[V](attrId: ObjId, valueType: AttrValueType[V]) =
    handlerLists.single(ToAttr(attrId,valueType), ()⇒throw new Exception(s"lost $attrId"))
  def converter[V](valueType: AttrValueType[V]): RawValueConverter[V] =
    handlerLists.single(ToRawValueConverter(valueType), ()⇒Never())
  def handlers[Value](attr: Attr[Value])(get: (Obj,ObjId)⇒Value): List[BaseCoHandler] = List(
    CoHandler(ToAttr(attrId(attr),valueType(attr)))(attr),
    CoHandler(GetValue(dbWrapType,attr))((obj,innerObj)⇒get(obj,innerObj.data))
  )
}

case class ToAttr[Value](attrId: ObjId, valueType: AttrValueType[Value]) extends EventKey[Attr[Value]]

class AttrImpl[Value](val id: ObjId, val valueType: AttrValueType[Value]) extends Attr[Value] {
  override def toString = s"Attr(${id.toString})"
}
