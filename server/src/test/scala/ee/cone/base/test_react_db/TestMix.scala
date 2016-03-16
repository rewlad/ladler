package ee.cone.base.test_react_db

import java.nio.file.Paths

import ee.cone.base.connection_api._
import ee.cone.base.db._
import ee.cone.base.lifecycle.{BaseConnectionMix, BaseAppMix}
import ee.cone.base.server.{ServerConnectionMix, ServerAppMix}
import ee.cone.base.vdom._

class TestAppMix extends BaseAppMix with ServerAppMix with DBAppMix {
  lazy val httpPort = 5557
  lazy val staticRoot = Paths.get("../client/build/test")
  lazy val ssePort = 5556
  lazy val threadCount = 50
  lazy val mainDB = new TestEnv[MainEnvKey](1L)
  lazy val instantDB = new TestEnv[InstantEnvKey](0L)
  lazy val createAlienConnection =
    (lifeCycle:LifeCycle) ⇒ new TestSessionConnectionMix(this, lifeCycle)
  lazy val createMergerConnection =
    (lifeCycle:LifeCycle) ⇒ new TestMergerConnectionMix(this, lifeCycle)
}

trait TestConnectionMix extends BaseConnectionMix with DBConnectionMix with VDomConnectionMix {
  lazy val testAttrs = new TestAttrs(
    attrFactory,
    labelFactory,
    searchIndex,
    definedValueConverter,
    nodeValueConverter,
    uuidValueConverter,
    stringValueConverter,
    mandatory,
    alienCanChange
  )()()
  lazy val tags = new Tags(childPairFactory, InputAttributesImpl, OnChangeImpl, OnClickImpl)

  override def handlers =
      testAttrs.handlers :::
      new TestView(
        testAttrs, alienAccessAttrs, handlerLists, findNodes, uniqueNodes, mainTx,
        tags, alienAttrFactory, currentView
      ).handlers :::
      //new DynEdit(sessionEventSourceOperations).handlers :::
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
    new Dumper().handlers :::
    new FailOfConnection(sender).handlers :::
    super.handlers

}

class TestMergerConnectionMix(
  app: TestAppMix, val lifeCycle: LifeCycle
) extends TestConnectionMix with MergerDBConnectionMix {
  def dbAppMix = app
}

object TestApp extends App {
  val app = new TestAppMix
  app.start()
  println(s"SEE: http://127.0.0.1:${app.httpPort}/react-app.html")
}

class Dumper extends CoHandlerProvider {
  private def dump(currentTx: CurrentTx[_]) = {
    val rawIndex = currentTx() match { case p: ProtectedBoundToTx[_] => p.rawIndex }
    val rawIndexes = rawIndex match {
      case i: MuxUnmergedIndex => i.unmerged :: i.merged :: Nil
    }
    val dataList = rawIndexes.collect {
      case i: TestIndex => ("MT",i.data)
      case i: NonEmptyUnmergedIndex => ("U",i.data)
    }
    for((hint,data) <- dataList){
      println(s"### $hint")
      for(pair <- data)
        println(s"${RawDumpImpl.apply(pair._1)} -> ${RawDumpImpl.apply(pair._2)}")
      println("###")
    }
  }
  def handlers = CoHandler(DumpKey)(dump) :: Nil
}

case object DumpKey extends EventKey[CurrentTx[_]=>Unit]