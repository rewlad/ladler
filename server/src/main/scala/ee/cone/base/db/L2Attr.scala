package ee.cone.base.db

import java.nio.ByteBuffer
import java.util.UUID

import ee.cone.base.connection_api.Attr
import ee.cone.base.util.{Never, HexDebug}

class AttrFactoryImpl(asBoolean: AttrValueType[Boolean])(val noAttr: NoAttr=NoAttr) extends AttrFactory {
  def apply[V](hiAttrId: Long, loAttrId: Long, valueType: AttrValueType[V]): Attr[V] with ObjId = {
    val booleanAttr = AttrImpl[Boolean](hiAttrId, loAttrId)(asBoolean, identity)
    AttrImpl(hiAttrId, loAttrId)(valueType, _=>booleanAttr)
  }
  private def apply[V](uuid: UUID, valueType: AttrValueType[V]): Attr[V] with ObjId =
    apply(uuid.getMostSignificantBits, uuid.getLeastSignificantBits, valueType)
  def apply[V](uuid: String, valueType: AttrValueType[V]): Attr[V] with ObjId =
    apply(UUID.fromString(uuid), valueType)
    //UUID.nameUUIDFromBytes()
  def derive[V](attrA: Attr[Boolean], attrB: Attr[V]) = {
    val rawAttrA = attrA.asInstanceOf[ObjId]
    val rawAttrB = attrB.asInstanceOf[ObjId with RawAttr[V]]
    val buffer = ByteBuffer.allocate(java.lang.Long.BYTES*4)
    buffer.putLong(rawAttrA.hi).putLong(rawAttrA.lo)
    buffer.putLong(rawAttrB.hi).putLong(rawAttrB.lo)
    apply(UUID.nameUUIDFromBytes(buffer.array()), rawAttrB.valueType)
  }
  def defined(attr: Attr[_]) = attr.asInstanceOf[DefinedAttr].defined
}

trait DefinedAttr { def defined: Attr[Boolean] }
trait NoAttr extends Attr[Boolean] with ObjId with DefinedAttr
case object NoAttr extends NoAttr {
  def defined = this
  def hi = Never()
  def lo = Never()
  def nonEmpty = false
}

case class AttrImpl[Value](hi: Long, lo: Long)(
  val valueType: AttrValueType[Value],
  getNonEmpty: Attr[Value]=>Attr[Boolean]
) extends Attr[Value] with ObjId with RawAttr[Value] with DefinedAttr {
  def defined: Attr[Boolean] = getNonEmpty(this)
  override def toString = s"AttrImpl(${HexDebug(hi)},${HexDebug(lo)})"
  def nonEmpty = true
}

