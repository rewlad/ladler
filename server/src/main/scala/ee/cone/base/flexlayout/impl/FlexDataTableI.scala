package ee.cone.base.flexlayout

import ee.cone.base.vdom.TagAttr

case class Width(value:Float) extends TagAttr
case class Priority(value:Int) extends TagAttr
case class MaxVisibleLines(value:Int) extends TagAttr
case class Toggled(value:Boolean)(val toggleHandle:Option[()=>Unit]) extends TagAttr
case class IsSelected(value:Boolean) extends TagAttr
