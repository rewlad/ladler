package ee.cone.base.db

import java.nio.ByteBuffer
import java.util.UUID

import ee.cone.base.connection_api.Attr
import ee.cone.base.util.Never

case object NoObjId extends ObjId {
  def hi: Long = Never()
  def lo: Long = Never()
  def nonEmpty = false
}
case class ObjIdImpl(hi: Long, lo: Long) extends ObjId {
  def nonEmpty = true
  override def toString = if(hi==0) super.toString else new UUID(hi,lo).toString
}
class ObjIdFactoryImpl extends ObjIdFactory {
  def noObjId = NoObjId
  def toObjId(hiObjId: Long, loObjId: Long) = new ObjIdImpl(hiObjId, loObjId)
  def toObjId(uuid: UUID): ObjId =
    toObjId(uuid.getMostSignificantBits, uuid.getLeastSignificantBits)
  def toObjId(uuid: String) = toObjId(UUID.fromString(uuid))
  def compose(objIds: List[ObjId]): ObjId = {
    val buffer = ByteBuffer.allocate(java.lang.Long.BYTES*2*objIds.size)
    objIds.foreach(objId⇒buffer.putLong(objId.hi).putLong(objId.lo))
    toObjId(UUID.nameUUIDFromBytes(buffer.array()))
  }
  def toUUIDString(objId: ObjId) = new UUID(objId.hi,objId.lo).toString
}

class NodeAttrsImpl(attr: AttrFactory, asDBNode: AttrValueType[ObjId])(
  val objId: Attr[ObjId] = attr("848ca1e3-e36b-4f9b-a39d-bd6b1d9bad98", asDBNode)
) extends NodeAttrs