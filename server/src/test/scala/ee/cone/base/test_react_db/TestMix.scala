package ee.cone.base.test_react_db

import java.nio.file.Paths

import ee.cone.base.connection_api.LifeCycle
import ee.cone.base.db.{MergerDBConnectionMix, SessionDBConnectionMix, DBAppMix}
import ee.cone.base.lifecycle.{BaseConnectionMix, BaseAppMix}
import ee.cone.base.server.{ServerConnectionMix, ServerAppMix}
import ee.cone.base.vdom.VDomConnectionMix

class TestAppMix extends BaseAppMix with ServerAppMix with DBAppMix {
  lazy val httpPort = 5557
  lazy val staticRoot = Paths.get("../client/build/test")
  lazy val ssePort = 5556
  lazy val threadCount = 5
  lazy val mainDB = new TestEnv
  lazy val instantDB = new TestEnv
  lazy val createAlienConnection =
    (lifeCycle:LifeCycle) ⇒ new TestSessionConnectionMix(this, lifeCycle)
  lazy val createMergerConnection =
    (lifeCycle:LifeCycle) ⇒ new TestMergerConnectionMix(this, lifeCycle)
}

class TestSessionConnectionMix(
  app: TestAppMix, val lifeCycle: LifeCycle
) extends BaseConnectionMix with ServerConnectionMix with SessionDBConnectionMix with VDomConnectionMix {
  lazy val serverAppMix = app
  lazy val dbAppMix = app
  lazy val allowOrigin = Some("*")
  lazy val framePeriod = 200L
  //lazy val mainDB = app.mainDB
  override def handlers =
    new FailOfConnection(sender).handlers :::
      new DynEdit(sessionEventSourceOperations).handlers :::
      super.handlers
}

class TestMergerConnectionMix(
  app: TestAppMix, val lifeCycle: LifeCycle
) extends BaseConnectionMix with MergerDBConnectionMix {
  def dbAppMix = app
}

object TestApp extends App {
  val app = new TestAppMix
  app.executionManager.start()
  println(s"SEE: http://127.0.0.1:${app.httpPort}/react-app.html")
}
