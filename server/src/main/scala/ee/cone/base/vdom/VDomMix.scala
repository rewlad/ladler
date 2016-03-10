package ee.cone.base.vdom

import ee.cone.base.connection_api.CoMixBase

trait VDomConnectionMix extends CoMixBase {
  lazy val diff = new DiffImpl(MapValueImpl,WasNoValueImpl)
  lazy val alienAttrFactory = new AlienAttrFactoryImpl(handlerLists,currentView)
  lazy val childPairFactory = new ChildPairFactoryImpl(MapValueImpl)
  lazy val currentView =
    new CurrentVDom(handlerLists,diff,JsonToStringImpl,WasNoValueImpl)
  override def handlers = currentView.handlers ::: super.handlers
}
