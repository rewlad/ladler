package ee.cone.base.test_sse

import java.nio.file.Paths

import ee.cone.base.server.{ContextOfConnection, SenderOfConnection, SSEHttpServer}


object TestApp extends App {
  val server = new SSEHttpServer {
    def threadCount = 5
    def allowOrigin = Some("*")
    def ssePort = 5556
    def httpPort = 5557
    def framePeriod = 20
    def purgePeriod = 2000
    def staticRoot = Paths.get("../client/build/test")
    def createMessageReceiverOfConnection(context: ContextOfConnection) =
      new TestFrameHandler(context.sender)
  }
  server.start()
  println(s"SEE: http://127.0.0.1:${server.httpPort}/sse.html")
}
