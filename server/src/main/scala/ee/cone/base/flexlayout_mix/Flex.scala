package ee.cone.base.flexlayout_impl

import ee.cone.base.vdom_impl.VDomConnectionMix

trait FlexConnectionMix extends VDomConnectionMix{
  lazy val flexTags = new FlexTagsImpl(childPairFactory,flexTablesState)
  lazy val flexTablesState = new FlexTablesState(currentView)
  lazy val flexTableTags = new FlexDataTableTagsImpl(childPairFactory,tags,tagStyles)
}
