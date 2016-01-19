package io.github.rewlad.ladler.test_sse

import java.nio.file.Paths

import io.github.rewlad.ladler.connection_api.SenderOfConnection
import io.github.rewlad.ladler.server.SSEHttpServer

object TestApp extends App {
  val server = new SSEHttpServer {
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
