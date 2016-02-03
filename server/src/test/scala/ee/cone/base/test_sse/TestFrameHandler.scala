package ee.cone.base.test_sse

import ee.cone.base.connection_api.ReceivedMessage
import ee.cone.base.server._

class TestFrameHandler(sender: SenderOfConnection) extends FrameHandler {
  private var prevTime: Long = 0L
  def frame(messages: List[ReceivedMessage]): Unit = {
    if(true){
      val time: Long = System.currentTimeMillis
      sender.send("show",s"$time")
    } else {
      val time: Long = System.currentTimeMillis / 100
      if(prevTime == time) return
      prevTime = time
      sender.send("show",s"$time")
    }
  }
}
