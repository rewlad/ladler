package ee.cone.base.test_sse

class TestFrameHandler(sender: SenderOfConnection) extends FrameHandler {
  private var prevTime: Long = 0L
  def frame(messageOption: Option[ReceivedMessage]): Unit = {
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
