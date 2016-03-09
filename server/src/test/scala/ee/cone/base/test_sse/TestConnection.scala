package ee.cone.base.test_sse

import ee.cone.base.connection_api._

class TestConnection(sender: SenderOfConnection) extends CoHandlerProvider {
  def handlers = CoHandler(FromAlienDictMessageKey){ messageOpt =>
    val time: Long = System.currentTimeMillis
    sender.sendToAlien("show",s"$time")
  } :: Nil
}
