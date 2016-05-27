package ee.cone.base.vdom

import ee.cone.base.connection_api.CoMixBase

trait VDomConnectionMix extends CoMixBase {
  lazy val diff = new DiffImpl(MapVDomValueImpl,WasNoValueImpl)
  lazy val childPairFactory = new ChildPairFactoryImpl(MapVDomValueImpl)
  lazy val currentView =
    new CurrentVDom(handlerLists,diff,JsonToStringImpl,WasNoValueImpl)
  lazy val tags = new TagsImpl(currentView,childPairFactory)
}
