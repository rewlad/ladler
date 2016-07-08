package ee.cone.base.vdom_impl

import ee.cone.base.connection_api.{Attr, AttrValueType, EventKey, Obj}
import ee.cone.base.vdom._
import ee.cone.base.vdom.Types.VDomKey

trait JsonToString {
  def apply(value: VDomValue): String
}

trait WasNoVDomValue extends VDomValue

trait VPair {
  def jsonKey: String
  def sameKey(other: VPair): Boolean
  def value: VDomValue
  def withValue(value: VDomValue): VPair
}

trait MapVDomValue extends VDomValue {
  def pairs: List[VPair]
}

trait Diff {
  def diff(vDom: VDomValue): Option[MapVDomValue]
}



////

trait RootWrap {
  def wrap(children: ()⇒List[ChildPair[OfDiv]]): List[ChildPair[OfDiv]]
}
trait ThemeRootWrap extends RootWrap
trait DBRootWrap extends RootWrap

////

trait FieldOption
trait Fields {
  def field[Value](obj: Obj, attr: Attr[Value], showLabel: Boolean, options: FieldOption*): List[ChildPair[OfDiv]]
}
case class ViewField[Value](asType: AttrValueType[Value])
  extends EventKey[(Obj,Attr[Value],Boolean,Seq[FieldOption])=>List[ChildPair[OfDiv]]]

trait PopupState
