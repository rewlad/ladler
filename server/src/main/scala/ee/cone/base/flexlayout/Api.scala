package ee.cone.base.flexlayout

import ee.cone.base.vdom.{ChildPair, OfDiv, TagAttr}
import ee.cone.base.vdom.Types._

trait OfFlexGrid
trait FlexTags {
  def flexGrid(key: VDomKey)(children: List[ChildPair[OfFlexGrid]]): ChildPair[OfDiv]
  def flexItem(key: VDomKey, flexBasisWidth: Int, maxWidth: Option[Int], sync: Boolean=false)(
      children: List[TagAttr]⇒List[ChildPair[OfDiv]]
  ): ChildPair[OfFlexGrid]
}

//

case class Priority(value:Int) extends TagAttr
case class MaxVisibleLines(value:Int) extends TagAttr
case class Toggled(value:Boolean)(val toggleHandle:Option[()=>Unit]) extends TagAttr
case class IsSelected(value:Boolean) extends TagAttr