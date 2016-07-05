package ee.cone.base.vdom

import ee.cone.base.connection_api.{Attr, AttrValueType, EventKey, Obj}
import ee.cone.base.vdom.Types.VDomKey

trait JsonToString {
  def apply(value: VDomValue): String
}

trait JsonBuilder {
  def startArray(): JsonBuilder
  def startObject(): JsonBuilder
  def end(): JsonBuilder
  def append(value: String): JsonBuilder
  def append(value: Boolean): JsonBuilder
}

trait ToJson {
  def appendJson(builder: JsonBuilder): Unit
}

trait VDomValue extends ToJson

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

case class ViewPath(path: String) extends EventKey[String=>List[ChildPair[_]]]

trait CurrentView {
  def invalidate(): Unit
  def until(value: Long): Unit
  def relocate(value: String): Unit
}

trait OnClickReceiver {
  def onClick: Option[()⇒Unit]
}

trait OnChangeReceiver {
  def onChange: Option[String⇒Unit]
}

trait OnResizeReceiver{
  def onResize: Option[String⇒Unit]
}

////

object Types {
  type VDomKey = String
}

trait ChildPairFactory {
  def apply[C](key: VDomKey, theElement: VDomValue, elements: List[ChildPair[_]]): ChildPair[C]
}

trait ChildPair[C] extends VPair {
  def key: VDomKey
}

abstract class TagName(val name: String)
trait TagJsonUtils {
  def appendInputAttributes(builder: JsonBuilder, value: String, deferSend: Boolean): Unit
}

////

trait RootWrap {
  def wrap(children: ()⇒List[ChildPair[OfDiv]]): List[ChildPair[OfDiv]]
}
trait ThemeRootWrap extends RootWrap
trait DBRootWrap extends RootWrap

trait OfDiv
trait Tags {
  def text(key: VDomKey, text: String): ChildPair[OfDiv]
  def tag(key: VDomKey, tagName: TagName, attr: TagStyle*)(children: List[ChildPair[OfDiv]]): ChildPair[OfDiv]
  def div(key: VDomKey, attr: TagStyle*)(children: List[ChildPair[OfDiv]]): ChildPair[OfDiv]
  def divButton(key:VDomKey)(action:()=>Unit)(children: List[ChildPair[OfDiv]]): ChildPair[OfDiv]
}
trait TagAttr
trait TagStyle extends TagAttr {
  def appendStyle(builder: JsonBuilder): Unit
}
trait Color {
  def value: String
}

////

trait FieldOption
trait Fields {
  def field[Value](obj: Obj, attr: Attr[Value], showLabel: Boolean, options: FieldOption*): List[ChildPair[OfDiv]]
}
case class ViewField[Value](asType: AttrValueType[Value])
  extends EventKey[(Obj,Attr[Value],Boolean,Seq[FieldOption])=>List[ChildPair[OfDiv]]]

trait PopupState

////

trait ChildOfTable
trait ChildOfTableRow

trait TableTags {
  type CellContentVariant = Boolean
  def table(key: VDomKey, attr: TagAttr*)(children:List[ChildOfTable]): List[ChildPair[OfDiv]]
  def row(key: VDomKey, attr: TagAttr*)(children:List[ChildOfTableRow]): ChildOfTable
  def group(key:VDomKey, attr: TagAttr*)(children:List[ChildPair[OfDiv]]): ChildOfTableRow
  def cell(key:VDomKey, attr: TagAttr*)(children:CellContentVariant=>List[ChildPair[OfDiv]]): ChildOfTableRow
}

case object IsHeader extends TagAttr
case class MaxWidth(value:Int) extends TagAttr
case class MinWidth(value:Int) extends TagAttr
case class Divider(value:VDomValue) extends TagAttr