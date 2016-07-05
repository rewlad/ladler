
package ee.cone.base.test_loots // demo

import java.nio.file.Paths
import java.time.{LocalTime, Duration, Instant}

import ee.cone.base.connection_api.{WrapType, LifeCycle}
import ee.cone.base.db._
import ee.cone.base.lifecycle.{BaseConnectionMix,BaseAppMix}
import ee.cone.base.lmdb.LightningDBAppMix
import ee.cone.base.server.{ServerConnectionMix, ServerAppMix}
import ee.cone.base.vdom.{TagJsonUtilsImpl,VDomConnectionMix}

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
  lazy val logAttributes = new BoatLogEntryAttributes(
    attrFactory,labelFactory,asDBObj,asString,asInstant,asLocalTime,asDuration,asBoolean
  )()
  lazy val materialIconTags = new MaterialIconTags(childPairFactory)
  lazy val materialTags = new MaterialTags(childPairFactory, TagJsonUtilsImpl, tags, popup)
  lazy val flexTags = new FlexTags(childPairFactory,tags,materialTags)
  lazy val dtTablesState=new DataTablesState(currentView)
  lazy val htmlTableWithControl = new FlexDataTableTagsImpl(flexTags)

  lazy val userAttrs = new UserAttrs(attrFactory, labelFactory, objIdFactory, asDBObj, asString, asUUID)()
  lazy val users = new Users(userAttrs, nodeAttrs, findAttrs, alienAttributes, handlerLists, factIndex, searchIndex, findNodes, mainTx, alien, transient, mandatory, unique, onUpdate, uiStrings, itemListOrderingFactory)()

  lazy val fuelingAttrs = new FuelingAttrs(attrFactory, labelFactory, objIdFactory, asString, asDuration, asBigDecimal, asDBObjId)()
  lazy val fuelingItems = new FuelingItems(
    fuelingAttrs, findAttrs, alienAttributes, nodeAttrs,
    factIndex, searchIndex, alien, onUpdate, attrFactory, dbWrapType, validationFactory, uiStrings, lazyObjFactory
  )()

  lazy val errorAttrs=new ErrorAttributes(attrFactory,labelFactory,asDBObj,asString,asBoolean)()
  lazy val errors = new Errors(errorAttrs,searchIndex,alien,users,findNodes,itemListFactory)()
  //

  lazy val popup = new Popup

  lazy val fieldAttributes = new FieldAttributesImpl(findAttrs,validationAttributes,alienAttributes)

  lazy val materialFields = new MaterialFields(
    fieldAttributes,
    handlerLists,
    tags,
    materialTags,
    materialIconTags,
    flexTags,
    popup,
    asDuration,
    asInstant,
    asLocalTime,
    asBigDecimal,
    asDBObj,
    asString,
    asBoolean
  )

  FieldsImpl
  WrapDBViewImpl

  lazy val testComponent = new TestComponent(
    nodeAttrs, findAttrs, itemListAttributes, logAttributes, userAttrs,
    fuelingAttrs, alienAttributes, validationAttributes,
    handlerLists,
    attrFactory,
    findNodes, mainTx, alien, onUpdate,
    tags, materialTags, flexTags, currentView, dtTablesState,
    searchIndex, factIndex, htmlTableWithControl, users, fuelingItems,
    objIdFactory, validationFactory,
    asDuration, asInstant, asLocalTime,asBigDecimal, asDBObj, asString, asUUID,
    uiStrings, mandatory, zoneIds, itemListOrderingFactory, objOrderingFactory,
    errorAttrs, errors, itemListFactory, filterObjFactory, editing, popup,
    materialIconTags
  )()
}

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
