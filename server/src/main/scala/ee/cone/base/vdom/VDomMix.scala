package ee.cone.base.vdom

import ee.cone.base.connection_api.{SenderOfConnection, CoMixBase}

trait VDomConnectionMix extends CoMixBase {
  def sender: SenderOfConnection

  lazy val diff = new DiffImpl(MapValueImpl,WasNoValueImpl)
  lazy val currentView =
    new CurrentVDom(handlerLists,diff,JsonToStringImpl,sender,WasNoValueImpl)
  override def handlers = currentView.handlers ::: super.handlers
}
