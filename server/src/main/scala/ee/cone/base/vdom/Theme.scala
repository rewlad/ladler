package ee.cone.base.vdom

import ee.cone.base.connection_api.{EventKey, AttrValueType, Attr, Obj}

case class ViewPath(path: String) extends EventKey[String=>List[ChildPair[_]]]

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

