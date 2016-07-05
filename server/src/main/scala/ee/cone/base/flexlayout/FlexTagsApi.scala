package ee.cone.base.flexlayout

import ee.cone.base.vdom.{ChildPair, OfDiv}
import ee.cone.base.vdom.Types._

trait FlexTags {
  def flexGrid(key: VDomKey)(children: List[ChildPair[OfDiv]]): ChildPair[OfDiv]
  def flexItem(key: VDomKey, flexBasisWidth: Int, maxWidth: Option[Int], onResize: Option[String=>Unit]=None)(children: List[ChildPair[OfDiv]]): ChildPair[OfDiv]
}
