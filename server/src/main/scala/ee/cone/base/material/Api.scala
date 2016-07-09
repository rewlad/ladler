
package ee.cone.base.material

import ee.cone.base.connection_api.{Obj, Attr, EventKey}
import ee.cone.base.vdom._
import ee.cone.base.vdom.Types._

trait ButtonTags {
  def checkBox(key:VDomKey,label:String,checked:Boolean,check:Boolean=>Unit): ChildPair[OfDiv]
  def raisedButton(key: VDomKey, label: String)(action: ()=>Unit): ChildPair[OfDiv]
  def iconButton(key: VDomKey, tooltip: String, picture: TagName)(action: ()=>Unit): ChildPair[OfDiv]
}

trait OptionTags {
  def option(key:VDomKey, caption: String)(activate: ()⇒Unit): ChildPair[OfDiv]
}

trait MaterialTags extends ThemeRootWrap {
  def materialChip(key:VDomKey,text:String)(action:Option[()=>Unit],children:List[ChildPair[OfDiv]]=Nil): ChildPair[OfDiv]
  def inset(key:VDomKey, children: List[ChildPair[OfDiv]]): ChildPair[OfDiv]
  def paperWithMargin(key: VDomKey, children: ChildPair[OfDiv]*): ChildPair[OfDiv]
  def alert(key:VDomKey, content:String): ChildPair[OfDiv]
  def helmet(title:String,addViewPort:Boolean = true): ChildPair[OfDiv]
  def toolbar(title:String): ChildPair[OfDiv]
  def notification(message:String, actionLabel:String = "",show:Boolean=true,close:()=>Unit): List[ChildPair[OfDiv]]
}

case object ToolbarButtons extends EventKey[()⇒List[ChildPair[OfDiv]]]
case object MenuItems extends EventKey[()⇒List[ChildPair[OfDiv]]]

case class EditableFieldOption(on: Boolean) extends FieldOption
case class DeferSendFieldOption(on: Boolean) extends FieldOption
case object IsPasswordFieldOption extends FieldOption
case object IsPersonFieldOption extends FieldOption

case class AttrValueOptions(attr: Attr[Obj]) extends EventKey[Obj⇒List[Obj]]

trait TableUtilTags {
  def controlPanel(left: List[ChildPair[OfDiv]], right: List[ChildPair[OfDiv]]): List[ChildPair[OfDiv]]
}