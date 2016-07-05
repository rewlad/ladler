package ee.cone.base.connection_api

class WrapType[WrapData]
trait Obj {
  def apply[Value](attr: Attr[Value]): Value
  def update[Value](attr: Attr[Value], value: Value): Unit
  def wrap[FWrapData](wrapType: WrapType[FWrapData], wrapData: FWrapData): Obj
}
trait Attr[Value]

trait ObjId {
  def hi: Long
  def lo: Long
  def nonEmpty: Boolean
}
case class AttrValueType[Value](id: ObjId)
case class ConverterKey[From,To](from: AttrValueType[From], to: AttrValueType[To])
  extends EventKey[From⇒To]
case class AttrCaption(attr: Attr[_]) extends EventKey[String]

case class ValidationState(objId: ObjId, attrId: ObjId, isError: Boolean, text: String)
trait ObjValidation {
  def get[Value](attr: Attr[Value]): List[ValidationState]
}

trait FieldAttributes {
  def aNonEmpty: Attr[Boolean]
  def aValidation: Attr[ObjValidation]
  def aIsEditing: Attr[Boolean]
  def aObjIdStr: Attr[String]
}