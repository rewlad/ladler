package ee.cone.base.test_sse

import java.net.Socket
import java.nio.file.Paths

import ee.cone.base.server._

class AppModule


object TestApp extends App {
  val server = new SSEHttpServer {
    def threadCount = 5
    def ssePort = 5556
    def httpPort = 5557
    def framePeriod = 20
    def staticRoot = Paths.get("../client/build/test")
    def createMessageReceiverOfConnection(context: ContextOfConnection) =
      new TestFrameHandler(context.sender)
    protected def createConnection(
      theConnectionLifeCycle: LifeCycle,
      theSocket: Socket
    ) = new ServerConnectionModule {

      lazy val connectionLifeCycle = theConnectionLifeCycle
      lazy val socket = theSocket
      lazy val allowOrigin = Some("*")
      lazy val purgePeriod = 2000
      def connectionRegistry = SSEHttpServer.this.conn
    }
  }
  server.start()
  println(s"SEE: http://127.0.0.1:${server.httpPort}/sse.html")
}
