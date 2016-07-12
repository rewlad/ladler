package ee.cone.base.test_react_db

import java.nio.file.Paths

import ee.cone.base.connection_api._
import ee.cone.base.db.{AlienAttributes, CurrentTx}
import ee.cone.base.lifecycle_mix.{BaseAppMix, BaseConnectionMix}
import ee.cone.base.server_mix.{ServerAppMix, ServerConnectionMix}
import ee.cone.base.db_mix._
import ee.cone.base.vdom_mix._

object TestApp extends App {
  val app = new TestAppMix
  app.start()
  println(s"SEE: http://127.0.0.1:${app.httpPort}/react-app.html#/test")
}

class TestAppMix extends BaseAppMix
  with ServerAppMix
  with InstantInMemoryDBAppMix
  with MainInMemoryDBAppMix
{
  lazy val httpPort = 5557
  lazy val staticRoot = Paths.get("../client/build/test")
  lazy val ssePort = 5556
  lazy val threadCount = 50
  lazy val createAlienConnection =
    (lifeCycle:LifeCycle) ⇒ new TestSessionConnectionMix(this, lifeCycle)
  lazy val createMergerConnection =
    (lifeCycle:LifeCycle) ⇒ new TestMergerConnectionMix(this, lifeCycle)
}

class FieldAttributesImpl(
  alienAttrs: AlienAttributes
) extends FieldAttributes {
  def aNonEmpty = ???
  def aValidation = ???
  def aIsEditing = ???
  def aObjIdStr: Attr[String] = alienAttrs.objIdStr
}

trait TestConnectionMix extends BaseConnectionMix with DBConnectionMix with VDomConnectionMix {
  lazy val testAttrs = new TestAttrs(objIdFactory,attrFactory,labelFactory,basicValueTypes)()
  lazy val testTags = new TestTags(childPairFactory, tagJsonUtils)
  lazy val fieldAttributes = new FieldAttributesImpl(alienAttributes)
  lazy val testComponent = new TestComponent(
    testAttrs, alienAttributes, handlerLists, objIdFactory,findNodes, mainTx,
    tags, testTags, currentView, searchIndex, mandatory, alien, factIndex, fieldAttributes
  )()
}

class TestSessionConnectionMix(
  app: TestAppMix, val lifeCycle: LifeCycle
) extends TestConnectionMix with SessionDBConnectionMix with ServerConnectionMix {
  lazy val serverAppMix = app
  lazy val dbAppMix = app
  lazy val allowOrigin = Some("*")
  lazy val framePeriod = 200L
  lazy val dumper = new Dumper()
  lazy val failOfConnection = new FailOfConnection(sender)
}

class TestMergerConnectionMix(
  app: TestAppMix, val lifeCycle: LifeCycle
) extends TestConnectionMix with MergerDBConnectionMix {
  def dbAppMix = app
}

class Dumper extends CoHandlerProvider {
  import ee.cone.base.db_impl._
  private def dump(currentTx: CurrentTx[_]) = {
    val rawIndex = currentTx() match { case p: ProtectedBoundToTx[_] => p.rawIndex }
    val rawIndexes = rawIndex match {
      case i: MuxUnmergedIndex => i.unmerged :: i.merged :: Nil
    }
    val dataList = rawIndexes.collect {
      case i: InMemoryMergedIndex => ("MT",i.data)
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