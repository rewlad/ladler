
package io.github.rewlad.sseserver

import java.util.concurrent.Executors

class TestFrameHandler(ctx: Context) extends FrameHandler {
  private var prevTime: Long = 0L
  def frame(messageOption: Option[ConnectionRegistry.Message]): Unit = {
    if(true){
      val time: Long = System.currentTimeMillis
      ctx[SenderOfConnection].send("show",s"$time")
    } else {
      val time: Long = System.currentTimeMillis / 100
      if(prevTime == time) return
      prevTime = time
      ctx[SenderOfConnection].send("show",s"$time")
    }
  }
}

object TestApp extends App {
  val schPool = Executors.newScheduledThreadPool(5)
  val sseConnections = new ConnectionRegistry

  new RHttpServer {
    def port = 5557
    def pool = schPool
    lazy val connectionRegistry = sseConnections
  }.start()

  new SSEServer {
    def pool = schPool
    def allowOrigin = Some("*")
    def port = 5556
    def connectionComponents(ctx: Context) =
      new FrameGenerator(ctx, schPool, 20, 2000) ::
        new TestFrameHandler(ctx) :: Nil
    lazy val connectionRegistry = sseConnections
  }.start()
}

