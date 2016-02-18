package ee.cone.base.test_sse

import java.net.Socket
import java.nio.file.{Path, Paths}

import ee.cone.base.server._

class AppModule

class TestAppMix(val args: List[AppComponent]) extends ServerAppMix {
  lazy val httpPort = 5557
  lazy val staticRoot = Paths.get("../client/build/test")
  lazy val ssePort = 5556
  lazy val threadCount = 5
  lazy val createSSEConnection = (cArgs:List[ConnectionComponent]) ⇒ new TestConnectionMix(this, cArgs)
  //def runConnection(theSocket: Socket) = new TestConnectionMix.run()
}

class TestConnectionMix(app: TestAppMix, val args: List[ConnectionComponent]) extends ServerConnectionMix {
  lazy val serverAppMix = app
  lazy val allowOrigin = Some("*")
  lazy val purgePeriod = 2000
  lazy val run = new TestConnection(connectionLifeCycle,sender)
}

object TestApp extends App {
  val app = new TestAppMix(Nil)
  app.start()
  println(s"SEE: http://127.0.0.1:${app.httpPort}/sse.html")
}

