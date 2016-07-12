package ee.cone.base.vdom_mix

import ee.cone.base.vdom_impl._
import ee.cone.base.connection_api.CoMixBase

trait VDomConnectionMix extends CoMixBase {
  lazy val diff = new DiffImpl(MapVDomValueImpl,WasNoValueImpl)
  lazy val childPairFactory = new ChildPairFactoryImpl(MapVDomValueImpl)
  lazy val currentView =
    new CurrentVDom(handlerLists,diff,JsonToStringImpl,WasNoValueImpl,childPairFactory)
  lazy val tagJsonUtils = TagJsonUtilsImpl
  lazy val tags = new TagsImpl(childPairFactory,tagJsonUtils)
  lazy val tagStyles = new TagStylesImpl
}
