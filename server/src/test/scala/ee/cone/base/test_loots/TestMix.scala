
package ee.cone.base.test_loots // demo

import java.nio.file.Paths
import java.time.{LocalTime, Duration, Instant}

import ee.cone.base.connection_api.{FieldAttributes, WrapType, LifeCycle}
import ee.cone.base.db._
import ee.cone.base.db_impl.{InheritAttrRuleImpl,
IndexedObjCollectionFactoryImpl, DBConnectionMix, InMemoryEnv}
import ee.cone.base.flexlayout_impl.{FlexConnectionMix,
FlexDataTableTagsImpl, FlexTablesState, FlexTagsImpl}
import ee.cone.base.framework.ErrorListView
import ee.cone.base.framework_impl._
import ee.cone.base.lifecycle_impl.{BaseConnectionMix,BaseAppMix}
import ee.cone.base.lmdb.LightningDBAppMix
import ee.cone.base.material_impl._
import ee.cone.base.server_impl.{ServerConnectionMix, ServerAppMix}
import ee.cone.base.vdom.{TableTags, DBRootWrap}
import ee.cone.base.vdom_impl.{TagStylesImpl, TagJsonUtilsImpl,
VDomConnectionMix}

object TestApp extends App {
  val app = new TestAppMix
  app.start()
  println(s"SEE: http://127.0.0.1:${app.httpPort}/material-app.html#/entryList")
}

class TestAppMix extends BaseAppMix with ServerAppMix with LightningDBAppMix {
  lazy val mainDB = new InMemoryEnv[MainEnvKey](1L,ordering)
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

trait TestConnectionMix
  extends BaseConnectionMix
    with DBConnectionMix
    with VDomConnectionMix
    with FlexConnectionMix
    with MaterialConnectionMix
{



  lazy val fields = new FieldsImpl()
  lazy val dataTableUtils = new DataTableUtilsImpl()
  lazy val measure = new MeasureImpl
  def errorListView: ErrorListView
  lazy val dbRootWrap = new DBRootWrapImpl(handlerLists,errorListView,userListView,currentView,tags,measure)
  lazy val userAttributes = new UserAttributesImpl(attrFactory, labelFactory, objIdFactory, asDBObj, asString, asUUID)()
  lazy val users = new UsersImpl(userAttributes, nodeAttrs, findAttrs, alienAttributes, handlerLists, factIndex, searchIndex, findNodes, mainTx, alien, transient, mandatory, unique, onUpdate, uiStrings, itemListOrderingFactory)()
  lazy val userListView = new UserListViewImpl(
    attrFactory,filterObjFactory,indexedObjCollectionFactory,
    itemListOrderingFactory,userAttributes,users,currentView,tags,tableTags,
    optionTags,buttonTags,materialTags,dataTableUtils,fields,fieldAttributes,
    tableUtilTags
  )







  lazy val logAttributes = new BoatLogEntryAttributes(
    attrFactory,labelFactory,asDBObj,asString,asInstant,asLocalTime,asDuration,asBoolean
  )()

  lazy val fuelingAttrs = new FuelingAttrs(attrFactory, labelFactory, objIdFactory, asString, asDuration, asBigDecimal, asDBObjId)()
  lazy val fuelingItems = new FuelingItems(
    fuelingAttrs, findAttrs, alienAttributes, nodeAttrs,
    factIndex, searchIndex, alien, onUpdate, attrFactory, dbWrapType, validationFactory, uiStrings, lazyObjFactory
  )()

  lazy val errorAttrs=new ErrorAttributes(attrFactory,labelFactory,asDBObj,asString,asBoolean)()
  lazy val errors = new Errors(errorAttrs,searchIndex,alien,users,findNodes,indexedObjCollectionFactory)()
  lazy val errorListView = new ErrorListViewImpl()

  lazy val fieldAttributes = new FieldAttributesImpl(findAttrs,validationAttributes,alienAttributes)



  def tableTags = materialTableTags

  lazy val testComponent = new TestComponent(
    handlerLists, nodeAttrs,logAttributes,fuelingAttrs,
    attrFactory,
    findNodes, alien, onUpdate,
    tags, currentView,
    searchIndex, factIndex, tableTags, users, fuelingItems,
    objIdFactory, validationFactory,
    uiStrings, mandatory, zoneIds, itemListOrderingFactory,
    indexedObjCollectionFactory, filterObjFactory, inheritAttrRule,
    dataTableUtils, tableUtilTags, fields, fieldAttributes, tagStyles,
    materialTags, flexTags, optionTags, buttonTags, tableUtilTags
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
