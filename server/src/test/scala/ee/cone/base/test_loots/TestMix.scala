package ee.cone.base.test_loots

import java.nio.file.Paths

import ee.cone.base.connection_api.LifeCycle
import ee.cone.base.db._
import ee.cone.base.lifecycle.{BaseConnectionMix,
BaseAppMix}
import ee.cone.base.lmdb.LightningDBEnv
import ee.cone.base.server.{ServerConnectionMix, ServerAppMix}
import ee.cone.base.vdom.{InputAttributesImpl,VDomConnectionMix}

object TestApp extends App {
  val app = new TestAppMix
  app.start()
  println(s"SEE: http://127.0.0.1:${app.httpPort}/material-app.html#/entryList")
}

class TestAppMix extends BaseAppMix with ServerAppMix with DBAppMix {
  lazy val mainDB = new InMemoryEnv[MainEnvKey](1L)
  //lazy val instantDB = new InMemoryEnv[InstantEnvKey](0L)
  lazy val instantDB = new LightningDBEnv[InstantEnvKey](0L,".",1L << 30, executionManager)

  lazy val httpPort = 5557
  lazy val staticRoot = Paths.get("../client/build/test")
  lazy val ssePort = 5556
  lazy val threadCount = 50
  lazy val createAlienConnection =
    (lifeCycle:LifeCycle) ⇒ new TestSessionConnectionMix(this, lifeCycle)
  lazy val createMergerConnection =
    (lifeCycle:LifeCycle) ⇒ new TestMergerConnectionMix(this, lifeCycle)
}

trait TestConnectionMix extends BaseConnectionMix with DBConnectionMix with VDomConnectionMix {
  lazy val instantValueConverter = new InstantValueConverter(InnerRawValueConverterImpl)
  lazy val durationValueConverter = new DurationValueConverter(InnerRawValueConverterImpl)

  lazy val testAttributes = new TestAttributes(attrFactory, labelFactory, stringValueConverter)()
  lazy val logAttributes = new BoatLogEntryAttributes(
    sysAttrs,attrFactory,labelFactory,searchIndex,
    definedValueConverter,nodeValueConverter,stringValueConverter,uuidValueConverter,instantValueConverter,durationValueConverter,
    alienCanChange
  )()()
  lazy val materialTags = new MaterialTags(childPairFactory, InputAttributesImpl)

  override def handlers =
    //testAttributes.handlers,
    logAttributes.handlers :::
    new TestComponent(testAttributes, logAttributes, alienAccessAttrs, handlerLists, findNodes, uniqueNodes, mainTx, alienAttrFactory, tags, materialTags, currentView).handlers :::
    super.handlers
}



class TestSessionConnectionMix(
  app: TestAppMix, val lifeCycle: LifeCycle
) extends TestConnectionMix with SessionDBConnectionMix with ServerConnectionMix {
  lazy val serverAppMix = app
  lazy val dbAppMix = app
  lazy val allowOrigin = Some("*")
  lazy val framePeriod = 200L
  override def handlers =
  //  new Dumper().handlers :::
      new FailOfConnection(sender).handlers :::
      super.handlers
}

class TestMergerConnectionMix(
  app: TestAppMix, val lifeCycle: LifeCycle
) extends TestConnectionMix with MergerDBConnectionMix {
  def dbAppMix = app
}
