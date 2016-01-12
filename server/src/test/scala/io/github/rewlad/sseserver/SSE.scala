
package io.github.rewlad.sseserver

import java.nio.file.Paths

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

object TestSSE extends App {
  val server = new SSERHttpServer {
    def threadCount = 5
    def allowOrigin = Some("*")
    def ssePort = 5556
    def httpPort = 5557
    def framePeriod = 20
    def purgePeriod = 2000
    def staticRoot = Paths.get("../client/build/test")
    def createFrameHandlerOfConnection(sender: SenderOfConnection) =
      new TestFrameHandler(sender)
  }
  server.start()
  println(s"SEE: http://127.0.0.1:${server.httpPort}/sse.html")
}
