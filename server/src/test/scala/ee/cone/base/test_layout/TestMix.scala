package ee.cone.base.test_layout

import java.nio.file.Paths

import ee.cone.base.connection_api.LifeCycle
import ee.cone.base.lifecycle.{BaseConnectionMix, BaseAppMix}
import ee.cone.base.server.{ServerConnectionMix, ServerAppMix}
import ee.cone.base.vdom.VDomConnectionMix

object TestApp extends App {
  val app = new TestAppMix
  app.start()
  println(s"SEE: http://127.0.0.1:${app.httpPort}/react-app.html#/test")
}

class TestAppMix extends BaseAppMix with ServerAppMix {
  lazy val httpPort = 5557
  lazy val staticRoot = Paths.get("../client/build/test")
  lazy val ssePort = 5556
  lazy val threadCount = 50 // raise with user count
  lazy val createAlienConnection =
    (lifeCycle:LifeCycle) ⇒ new TestSessionConnectionMix(this, lifeCycle)
}

trait TestConnectionMix extends BaseConnectionMix with VDomConnectionMix {
  lazy val flexTags = new FlexTags(childPairFactory)
  lazy val materialTags = new MaterialTags(childPairFactory)
  lazy val testComponent = new TestComponent(tags,flexTags,materialTags)
}

class TestSessionConnectionMix(
  app: TestAppMix, val lifeCycle: LifeCycle
) extends TestConnectionMix with ServerConnectionMix {
  lazy val serverAppMix = app
  lazy val allowOrigin = Some("*")
  lazy val framePeriod = 200L
}
