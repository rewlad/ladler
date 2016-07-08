
package ee.cone.base.material_impl

import ee.cone.base.vdom.TagName

case object IconContentSave extends TagName("IconContentSave")
case object IconActionRestore extends TagName("IconActionRestore")
case object IconContentRemove extends TagName("IconContentRemove")

case object IconNavigationDropDown extends TagName("IconNavigationDropDown")
case object IconNavigationDropUp extends TagName("IconNavigationDropUp")
case object IconContentAdd extends TagName("IconContentAdd")
case object IconEditorModeEdit extends TagName("IconEditorModeEdit")
case object IconActionDelete extends TagName("IconActionDelete")
// IconActionViewList IconContentFilterList IconContentClear

class TableIconTagsImpl extends TableIconTags {
  def iconArrowUp = IconNavigationDropDown
  def iconArrowDown = IconNavigationDropUp
  def iconAdd = IconContentAdd
  def iconModeEdit = IconEditorModeEdit
  def iconDelete = IconActionDelete
}

class EventIconTagsImpl extends EventIconTags {
  def iconSave = IconContentSave
  def iconEvents = IconActionRestore
  def iconRemove = IconContentRemove
}
