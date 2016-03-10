package ee.cone.base.test_sse

import java.nio.file.Paths

import ee.cone.base.connection_api.LifeCycle
import ee.cone.base.lifecycle.{BaseConnectionMix, BaseAppMix}
import ee.cone.base.server._

object TestApp extends App {
  val app = new TestAppMix
  app.start()
  println(s"SEE: http://127.0.0.1:${app.httpPort}/react-app.html")
}

class TestAppMix extends BaseAppMix with ServerAppMix {
  lazy val httpPort = 5557
  lazy val staticRoot = Paths.get("../client/build/test")
  lazy val ssePort = 5556
  lazy val threadCount = 5
  lazy val createAlienConnection =
    (lifeCycle: LifeCycle) ⇒ new TestConnectionMix(this, lifeCycle)
}

class TestConnectionMix(
  app: TestAppMix, val lifeCycle: LifeCycle
) extends BaseConnectionMix with ServerConnectionMix {
  lazy val serverAppMix = app
  lazy val allowOrigin = Some("*")
  lazy val framePeriod = 200L
  override def handlers =
    new TestConnection().handlers :::
      super.handlers
}

