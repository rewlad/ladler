package ee.cone.base.test_loots

import java.nio.file.Paths
import java.time.{Duration, Instant}

import ee.cone.base.connection_api.{WrapType, LifeCycle}
import ee.cone.base.db._
import ee.cone.base.lifecycle.{BaseConnectionMix,BaseAppMix}
import ee.cone.base.lmdb.LightningDBAppMix
import ee.cone.base.server.{ServerConnectionMix, ServerAppMix}
import ee.cone.base.vdom.{InputAttributesImpl,VDomConnectionMix}

object TestApp extends App {
  val app = new TestAppMix
  app.start()
  println(s"SEE: http://127.0.0.1:${app.httpPort}/material-app.html#/entryList")
}

class TestAppMix extends BaseAppMix with ServerAppMix with LightningDBAppMix {
  lazy val mainDB = new InMemoryEnv[MainEnvKey](1L)
  // lazy val instantDB = new InMemoryEnv[InstantEnvKey](0L)

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
  lazy val asInstant = new AttrValueType[Option[Instant]]
  lazy val asDuration = new AttrValueType[Option[Duration]]

  lazy val testAttributes = new TestAttributes(attrFactory, labelFactory, asString)()
  lazy val logAttributes = new BoatLogEntryAttributes(
    attrFactory,labelFactory,asDBObj,asString,asInstant,asDuration
  )()
  lazy val materialTags = new MaterialTags(childPairFactory, InputAttributesImpl)
  lazy val flexTags = new FlexTags(childPairFactory,tags,materialTags)
  lazy val dtTablesState=new DataTablesState(currentView)
  lazy val asObjIdSet = new AttrValueType[Set[ObjId]]
  lazy val listedWrapType = new ListedWrapType
  lazy val filterAttrs = new FilterAttrs(attrFactory, labelFactory, asString, asDefined, asObjIdSet)()
  lazy val filters = new Filters(filterAttrs,nodeAttrs,handlerLists,attrFactory,findNodes,mainTx,alienCanChange,listedWrapType,factIndex,searchIndex)()
  //
  lazy val instantValueConverter = new InstantValueConverter(asInstant,rawConverter)
  lazy val durationValueConverter = new DurationValueConverter(asDuration,rawConverter)
  lazy val objIdSetValueConverter = new ObjIdSetValueConverter(asObjIdSet,rawConverter,findNodes)
  lazy val testComponent = new TestComponent(
    nodeAttrs, findAttrs, filterAttrs, testAttributes, logAttributes,
    handlerLists,
    attrFactory,
    findNodes, mainTx, alienCanChange, onUpdate,
    tags, materialTags, flexTags, currentView, dtTablesState,
    searchIndex, factIndex, filters
  )()
}

class ListedWrapType extends WrapType[InnerItemList]

class TestSessionConnectionMix(
  app: TestAppMix, val lifeCycle: LifeCycle
) extends TestConnectionMix with SessionDBConnectionMix with ServerConnectionMix {
  lazy val serverAppMix = app
  lazy val dbAppMix = app
  lazy val allowOrigin = Some("*")
  lazy val framePeriod = 200L
  lazy val failOfConnection = new FailOfConnection(sender)
}

class TestMergerConnectionMix(
  app: TestAppMix, val lifeCycle: LifeCycle
) extends TestConnectionMix with MergerDBConnectionMix {
  def dbAppMix = app
}
