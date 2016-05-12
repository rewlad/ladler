package ee.cone.base.db

import java.nio.ByteBuffer
import java.util.UUID

import ee.cone.base.connection_api.{Obj, Attr}
import ee.cone.base.util.{HexDebug, Hex, Never}

class AttrFactoryImpl(asBoolean: AttrValueType[Boolean])(val noAttr: NoAttr=NoAttr) extends AttrFactory {
  def apply[V](hiAttrId: HiAttrId, loAttrId: LoAttrId, valueType: AttrValueType[V]): Attr[V] with RawAttr[V] = {
    val booleanAttr = AttrImpl[Boolean](hiAttrId, loAttrId)(asBoolean, identity)
    AttrImpl(hiAttrId, loAttrId)(valueType, _=>booleanAttr)
  }
  private def apply[V](uuid: UUID, valueType: AttrValueType[V]): Attr[V] with RawAttr[V] =
    apply(new HiAttrId(uuid.getMostSignificantBits), new LoAttrId(uuid.getLeastSignificantBits), valueType)
  def apply[V](uuid: String, valueType: AttrValueType[V]): Attr[V] with RawAttr[V] =
    apply(UUID.fromString(uuid), valueType)
    //UUID.nameUUIDFromBytes()
  def derive[V](attrA: Attr[Boolean], attrB: Attr[V]) = {
    val rawAttrA = attrA.asInstanceOf[RawAttr[Boolean]]
    val rawAttrB = attrB.asInstanceOf[RawAttr[V]]
    val buffer = ByteBuffer.allocate(java.lang.Long.BYTES*4)
    buffer.putLong(rawAttrA.hiAttrId.value).putLong(rawAttrA.loAttrId.value)
    buffer.putLong(rawAttrB.hiAttrId.value).putLong(rawAttrB.loAttrId.value)
    apply(UUID.nameUUIDFromBytes(buffer.array()), rawAttrB.valueType)
  }
  def defined(attr: Attr[_]) = attr.asInstanceOf[DefinedAttr].defined
}

trait DefinedAttr { def defined: Attr[Boolean] }
trait NoAttr extends Attr[Boolean] with DefinedAttr
case object NoAttr extends NoAttr {
  def defined = this
}

case class AttrImpl[Value](hiAttrId: HiAttrId, loAttrId: LoAttrId)(
  val valueType: AttrValueType[Value],
  getNonEmpty: Attr[Value]=>Attr[Boolean]
) extends Attr[Value] with RawAttr[Value] with DefinedAttr {
  def defined: Attr[Boolean] = getNonEmpty(this)
  override def toString = s"AttrImpl(${HexDebug(hiAttrId.value)},${HexDebug(loAttrId.value)})"
}

