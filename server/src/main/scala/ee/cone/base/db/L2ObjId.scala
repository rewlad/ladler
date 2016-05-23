package ee.cone.base.db

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
  def toObjId(uuid: UUID) = toObjId(uuid.getMostSignificantBits, uuid.getLeastSignificantBits)
  def toObjId(uuid: String) = toObjId(UUID.fromString(uuid))
}

class NodeAttrsImpl(attr: AttrFactory, asDBNode: AttrValueType[ObjId])(
  val objId: Attr[ObjId] = attr("848ca1e3-e36b-4f9b-a39d-bd6b1d9bad98", asDBNode)
) extends NodeAttrs