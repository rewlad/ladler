package ee.cone.base.material_impl

import ee.cone.base.material.OptionTags
import ee.cone.base.vdom._
import ee.cone.base.vdom.Types.VDomKey

trait OptionTagsI extends OptionTags {
  def popupBox(key: VDomKey, fieldChildren: List[ChildPair[OfDiv]], popupChildren: List[ChildPair[OfDiv]]): ChildPair[OfDiv]
  def popupAction[Value](key: PopupState): (Boolean,()=>Unit)
}

trait MaterialStyles {
  def paddingSide: Int⇒TagStyle
  def marginSide: TagStyle
}
