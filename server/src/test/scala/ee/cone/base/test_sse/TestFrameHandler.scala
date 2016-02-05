package ee.cone.base.test_sse

import ee.cone.base.connection_api.Message
import ee.cone.base.server._

class TestFrameHandler(sender: SenderOfConnection) extends ReceiverOf[Message] {
  private var prevTime: Long = 0L
  def receive = {
    case PeriodicMessage =>
      if(true){
        val time: Long = System.currentTimeMillis
        sender.send("show",s"$time")
      } else {
        val time: Long = System.currentTimeMillis / 100
        if(prevTime != time) {
          prevTime = time
          sender.send("show",s"$time")
        }
      }
    case _ => ()
  }
}
