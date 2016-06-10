package ee.cone.base.test_loots

import java.nio.file.Paths
import java.time.{LocalTime, Duration, Instant}

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
  lazy val asInstant = AttrValueType[Option[Instant]](objIdFactory.toObjId("ce152d2d-d783-439f-a21b-e175663f2650"))
  lazy val asDuration = AttrValueType[Option[Duration]](objIdFactory.toObjId("356068df-ac9d-44cf-871b-036fa0ac05ad"))
  lazy val asLocalTime = AttrValueType[Option[LocalTime]](objIdFactory.toObjId("8489d9a9-37ec-4206-be73-89287d0282e3"))
  lazy val asBigDecimal = new AttrValueType[Option[BigDecimal]](objIdFactory.toObjId("4d5894bc-e913-4d90-8b7f-32bd1d3893ea"))
  lazy val logAttributes = new BoatLogEntryAttributes(
    attrFactory,labelFactory,asDBObj,asString,asInstant,asLocalTime,asDuration,asBoolean
  )()
  lazy val materialTags = new MaterialTags(childPairFactory, InputAttributesImpl, tags)
  lazy val flexTags = new FlexTags(childPairFactory,tags,materialTags)
  lazy val dtTablesState=new DataTablesState(currentView)
  lazy val asObjIdSet = AttrValueType[Set[ObjId]](objIdFactory.toObjId("ca3fd9c9-870f-4604-8fe2-a6ae98b37c29"))
  lazy val listedWrapType = new ListedWrapType
  lazy val filterAttrs = new FilterAttrs(attrFactory, labelFactory, asBoolean, asDBObjId, asObjIdSet, asInstant)()
  lazy val filters = new Filters(filterAttrs,nodeAttrs,findAttrs,alienAttrs,handlerLists,attrFactory,findNodes,mainTx,alien,listedWrapType,factIndex,searchIndex,transient,objIdFactory)()
  lazy val htmlTableWithControl = new FlexDataTableImpl(flexTags)

  lazy val asObjValidation = AttrValueType[ObjValidation](objIdFactory.toObjId("f3ef68d8-60d3-4811-9db1-d187228feb89"))
  lazy val validationAttributes = new ValidationAttributes(attrFactory,asObjValidation)()
  lazy val validationWrapType = new ValidationWrapType
  lazy val validationFactory = new ValidationFactory(validationAttributes,nodeAttrs,attrFactory,dbWrapType,validationWrapType,uiStrings)()

  lazy val objOrderingFactory = new ObjOrderingFactory(handlerLists, attrFactory)
  lazy val objOrderingForAttrValueTypes = new ObjOrderingForAttrValueTypes(objOrderingFactory, asBoolean, asString, asDBObj, asInstant, asLocalTime, asBigDecimal, uiStrings)
  lazy val orderingAttributes = new ItemListOrderingAttributes(attrFactory, asBoolean, asDBObjId)()
  lazy val itemListOrderingFactory = new ItemListOrderingFactory(orderingAttributes, uiStringAttributes, attrFactory, factIndex, alien, objOrderingFactory)

  lazy val userAttrs = new UserAttrs(attrFactory, labelFactory, objIdFactory, asDBObj, asString, asUUID)()
  lazy val users = new Users(userAttrs, nodeAttrs, findAttrs, alienAttrs, handlerLists, factIndex, searchIndex, findNodes, mainTx, alien, transient, mandatory, unique, onUpdate, filters, uiStrings, itemListOrderingFactory)()
  lazy val fuelingAttrs = new FuelingAttrs(attrFactory, labelFactory, objIdFactory, asString, asDuration, asBigDecimal)()
  lazy val fuelingItems = new FuelingItems(
    fuelingAttrs, findAttrs, alienAttrs, filterAttrs, nodeAttrs,
    factIndex, searchIndex, alien, filters, onUpdate, attrFactory, dbWrapType, validationFactory, uiStrings
  )()
  lazy val zoneIds = new ZoneIds
  lazy val instantValueConverter = new InstantValueConverter(asInstant,rawConverter,asString,zoneIds)
  lazy val durationValueConverter = new DurationValueConverter(asDuration,rawConverter,asString)
  lazy val localTimeValueConverter = new LocalTimeValueConverter(asLocalTime,rawConverter,asString)
  lazy val objIdSetValueConverter = new ObjIdSetValueConverter(asObjIdSet,rawConverter,objIdFactory)
  lazy val bigDecimalValueConverter = new BigDecimalValueConverter(asBigDecimal,rawConverter,asString)
  lazy val testComponent = new TestComponent(
    nodeAttrs, findAttrs, filterAttrs, logAttributes, userAttrs,
    fuelingAttrs, alienAttrs, validationAttributes,
    handlerLists,
    attrFactory,
    findNodes, mainTx, alien, onUpdate,
    tags, materialTags, flexTags, currentView, dtTablesState,
    searchIndex, factIndex, filters, htmlTableWithControl, users, fuelingItems,
    objIdFactory, validationFactory,
    asDuration, asInstant, asLocalTime, asBigDecimal, asDBObj, asString, asUUID,
    uiStrings, mandatory, zoneIds, itemListOrderingFactory, objOrderingFactory
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
