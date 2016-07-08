package ee.cone.base.material_impl

import ee.cone.base.vdom._
import ee.cone.base.vdom.Types.VDomKey

trait ButtonTags {
  def checkBox(key:VDomKey,label:String,checked:Boolean,check:Boolean=>Unit): ChildPair[OfDiv]
  def raisedButton(key: VDomKey, label: String)(action: ()=>Unit): ChildPair[OfDiv]
  def iconButton(key: VDomKey, tooltip: String, picture: TagName)(action: ()=>Unit): ChildPair[OfDiv]
}

trait OptionTags {
  def option(key:VDomKey, caption: String)(activate: ()⇒Unit): ChildPair[OfDiv]
  def popupBox(key: VDomKey, fieldChildren: List[ChildPair[OfDiv]], popupChildren: List[ChildPair[OfDiv]]): ChildPair[OfDiv]
  def popupAction[Value](key: PopupState): (Boolean,()=>Unit)
}

trait MaterialStyles {
  def paddingSide: Int⇒TagStyle
  def marginSide: TagStyle
}
