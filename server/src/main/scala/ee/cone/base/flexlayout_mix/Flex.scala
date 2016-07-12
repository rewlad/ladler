package ee.cone.base.flexlayout_mix

import ee.cone.base.flexlayout_impl._
import ee.cone.base.vdom_mix.VDomConnectionMix

trait FlexConnectionMix extends VDomConnectionMix{
  lazy val flexTags = new FlexTagsImpl(childPairFactory,flexTablesState)
  lazy val flexTablesState = new FlexTablesState(currentView)
  lazy val flexTableTags = new FlexDataTableTagsImpl(childPairFactory,tags,tagStyles)
}
