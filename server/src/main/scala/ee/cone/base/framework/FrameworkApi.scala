package ee.cone.base.framework

import ee.cone.base.vdom.{ChildPair, OfDiv}

trait ErrorListView {
  def errorNotification: List[ChildPair[OfDiv]]
}

