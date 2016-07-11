
package ee.cone.base.test_loots // demo

import java.nio.file.Paths

import ee.cone.base.connection_api.LifeCycle
import ee.cone.base.db._
import ee.cone.base.db_impl._
import ee.cone.base.framework_impl._
import ee.cone.base.lifecycle_impl.BaseAppMix
import ee.cone.base.lmdb.LightningDBAppMix
import ee.cone.base.server_impl.{ServerAppMix, ServerConnectionMix}

object TestApp extends App {
  val app = new TestAppMix
  app.start()
  println(s"SEE: http://127.0.0.1:${app.httpPort}/material-app.html#/entryList")
}

class TestAppMix extends BaseAppMix with ServerAppMix with LightningDBAppMix {
  lazy val mainDB = new InMemoryEnv[MainEnvKey](1L,unsignedBytesOrdering)
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

trait TestConnectionMix extends FrameworkConnectionMix {
  lazy val logAttributes = new BoatLogEntryAttributes(attrFactory,labelFactory,basicValueTypes)()
  lazy val fuelingAttributes = new FuelingAttrs(attrFactory, labelFactory, objIdFactory, asDBObjId, basicValueTypes)()
  lazy val fuelingItems = new FuelingItems(
    fuelingAttributes, findAttrs, alienAttributes, nodeAttrs,
    factIndex, searchIndex, alien, onUpdate, attrFactory, dbWrapType, validationFactory, uiStrings, lazyObjFactory
  )()

  lazy val errorAttributes = new ErrorAttributes(attrFactory,labelFactory,basicValueTypes)()
  lazy val errors = new Errors(errorAttributes,searchIndex,alien,users,findNodes,indexedObjCollectionFactory)()
  lazy val errorListView = new ErrorListViewImpl(
    attrFactory,filterObjFactory,indexedObjCollectionFactory,itemListOrderingFactory,
    currentView,tags,tagStyles,
    materialTags,optionTags,tableUtilTags,
    tableTags,dataTableUtils,fieldAttributes,fields,
    errorAttributes,errors,users
  )

  lazy val testComponent = new TestComponent(
    handlerLists,

    nodeAttrs, objIdFactory, attrFactory, findNodes,
    alien, onUpdate, searchIndex, factIndex,
    validationFactory, uiStrings, mandatory, zoneIds,
    itemListOrderingFactory, indexedObjCollectionFactory, filterObjFactory, inheritAttrRule,

    currentView, tags, tagStyles,

    tableTags, dataTableUtils, fieldAttributes, fields, users,

    flexTags, materialTags, optionTags, buttonTags, tableUtilTags,

    logAttributes,fuelingAttributes,fuelingItems
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
