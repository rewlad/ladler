package ee.cone.base.db

import java.nio.ByteBuffer
import java.util.UUID

import ee.cone.base.connection_api.{CoHandlerLists, Attr}
import ee.cone.base.util.Never

class AttrFactoryImpl(
  handlerLists: CoHandlerLists,
  objIdFactory: ObjIdFactory
) extends AttrFactory {
  def apply[V](uuid: String, valueType: AttrValueType[V]): Attr[V] =
    define(objIdFactory.toObjId(uuid), valueType)
  def define[V](attrId: ObjId, valueType: AttrValueType[V]): Attr[V] =
    new AttrImpl(attrId, valueType)
  def derive[V](attrAId: ObjId, attrB: Attr[V]) = {
    val attrBId = attrId(attrB)
    val buffer = ByteBuffer.allocate(java.lang.Long.BYTES*4)
    buffer.putLong(attrAId.hi).putLong(attrAId.lo)
    buffer.putLong(attrBId.hi).putLong(attrBId.lo)
    define(objIdFactory.toObjId(UUID.nameUUIDFromBytes(buffer.array())), valueType(attrB))
  }
  def defined(attrId: ObjId) = handlerLists.single(ToDefined(attrId), ()⇒Never())
  def attrId[V](attr: Attr[V]): ObjId = attr.asInstanceOf[AttrImpl[V]].id
  def valueType[V](attr: Attr[V]): AttrValueType[V] = attr.asInstanceOf[AttrImpl[V]].valueType
  def converter[V](attr: Attr[V]): RawValueConverter[V] =
    handlerLists.single(ToRawValueConverter(valueType(attr)), ()⇒Never())
}

class AttrImpl[Value](val id: ObjId, val valueType: AttrValueType[Value]) extends Attr[Value]
