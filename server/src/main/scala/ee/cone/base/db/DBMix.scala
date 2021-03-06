package ee.cone.base.db

import java.util.UUID
import java.util.concurrent.ExecutorService

import ee.cone.base.connection_api._

trait DBAppMix extends AppMixBase {
  def mainDB: DBEnv[MainEnvKey]
  def instantDB: DBEnv[InstantEnvKey]
  def createMergerConnection: LifeCycle=>CoMixBase
  lazy val mergerCurrentRequest = new CurrentRequest(NoObjId)
  override def toStart =
    new Merger(executionManager,createMergerConnection) :: super.toStart
}

trait InMemoryDBAppMix extends DBAppMix {
  lazy val mainDB = new InMemoryEnv[MainEnvKey](1L)
  lazy val instantDB = new InMemoryEnv[InstantEnvKey](0L)
}

trait DBConnectionMix extends CoMixBase {
  def dbAppMix: DBAppMix
  def lifeCycle: LifeCycle

  // L0/L1
  lazy val rawConverter = new RawConverterImpl
  lazy val rawVisitor = new RawVisitorImpl
  // L2
  lazy val noObj = new NoObjImpl(handlerLists)

  lazy val objIdFactory = new ObjIdFactoryImpl

  lazy val asDefined = AttrValueType[Boolean](objIdFactory.toObjId("f8857bde-f26c-43ce-a1cd-a9091bcfdc23"))
  lazy val asDBObjId = AttrValueType[ObjId](objIdFactory.toObjId("8619613c-069d-473f-97f5-87d23a881a04"))
  lazy val dbWrapType = new DBWrapType

  lazy val attrFactory = new AttrFactoryImpl(handlerLists,objIdFactory,dbWrapType)
  lazy val nodeAttrs = new NodeAttrsImpl(attrFactory, asDBObjId)()

  lazy val dbObjIdValueConverter = new DBObjIdValueConverter(asDBObjId,rawConverter,objIdFactory)

  lazy val zeroNode = ObjIdImpl(0L,0L)
  lazy val factIndex =
    new FactIndexImpl(rawConverter, dbObjIdValueConverter, rawVisitor, handlerLists, nodeAttrs, attrFactory, objIdFactory, zeroNode, asDefined, dbWrapType)
  lazy val onUpdate = new OnUpdateImpl(attrFactory,factIndex)
  lazy val searchIndex =
    new SearchIndexImpl(handlerLists, rawConverter, dbObjIdValueConverter, rawVisitor, attrFactory, nodeAttrs, objIdFactory, onUpdate)

  // L3
  lazy val instantTx = new CurrentTxImpl[InstantEnvKey](dbAppMix.instantDB)
  lazy val mainTx = new CurrentTxImpl[MainEnvKey](dbAppMix.mainDB)
  lazy val txSelector = new TxSelectorImpl(nodeAttrs, instantTx, mainTx)

  lazy val asString = AttrValueType[String](objIdFactory.toObjId("1e94f9bc-a34d-4fab-8a01-eb3dd98795d2"))
  lazy val findAttrs = new FindAttrsImpl(attrFactory,asDefined,asString)()
  lazy val findNodes = new FindNodesImpl(findAttrs, handlerLists, nodeAttrs, noObj, attrFactory, objIdFactory, dbObjIdValueConverter, dbWrapType)()

  lazy val preCommitCheckCheckAll = new PreCommitCheckAllOfConnectionImpl(txSelector)
  lazy val mandatory = new MandatoryImpl(attrFactory, factIndex, preCommitCheckCheckAll)
  lazy val unique = new UniqueImpl(attrFactory, factIndex, txSelector, preCommitCheckCheckAll, searchIndex, findNodes)

  lazy val asDBObj = AttrValueType[Obj](objIdFactory.toObjId("275701ec-cb9b-4474-82e6-69f2e1f28c87"))
  lazy val asUUID = AttrValueType[Option[UUID]](objIdFactory.toObjId("13c5769d-f120-4a1a-9fce-c56df8835f08"))
  lazy val asBoolean = AttrValueType[Boolean](objIdFactory.toObjId("fa03f6f1-90ef-460d-a4dd-2279269a4d79"))
  lazy val labelFactory = new LabelFactoryImpl(attrFactory,asDBObj)

  lazy val instantTxManager =
    new DefaultTxManagerImpl[InstantEnvKey](lifeCycle, dbAppMix.instantDB, instantTx, preCommitCheckCheckAll)

  // Sys
  lazy val eventSourceAttrs =
    new EventSourceAttrsImpl(objIdFactory,attrFactory,labelFactory,asDBObj,asDBObjId,asUUID,asString)()
  lazy val eventSourceOperations =
    new EventSourceOperationsImpl(eventSourceAttrs,nodeAttrs,findAttrs,factIndex,handlerLists,findNodes,instantTx,mainTx,searchIndex,mandatory)()

  lazy val uiStringAttributes = new UIStringAttributes(attrFactory, asString)()
  lazy val uiStrings = new UIStringsImpl(uiStringAttributes, nodeAttrs, handlerLists, objIdFactory, attrFactory, factIndex, onUpdate, findNodes, asDBObj, asDBObjId, asString)

  lazy val transient = new TransientImpl(handlerLists, attrFactory, dbWrapType)

  lazy val alienAttrs = new AlienAccessAttrs(objIdFactory, attrFactory, asDBObj, asString, asBoolean)()
  lazy val alienWrapType = new AlienWrapType
  lazy val demandedWrapType = new DemandedWrapType
  lazy val alien = new Alien(alienAttrs,nodeAttrs,uiStringAttributes,attrFactory,handlerLists,findNodes,mainTx,factIndex,alienWrapType,demandedWrapType,objIdFactory,uiStrings,asDBObj,asString,transient)

  lazy val definedValueConverter = new DefinedValueConverter(asDefined, rawConverter)
  lazy val booleanValueConverter = new BooleanValueConverter(asBoolean, rawConverter)
  lazy val dbObjValueConverter = new DBObjValueConverter(asDBObj,dbObjIdValueConverter,findNodes,nodeAttrs)
  lazy val uuidValueConverter = new UUIDValueConverter(asUUID,rawConverter)
  lazy val stringValueConverter = new StringValueConverter(asString,rawConverter)
}

trait MergerDBConnectionMix extends DBConnectionMix {
  lazy val currentRequest = dbAppMix.mergerCurrentRequest
  lazy val mainTxManager =
    new DefaultTxManagerImpl[MainEnvKey](lifeCycle, dbAppMix.mainDB, mainTx, preCommitCheckCheckAll)
  lazy val mergerEventSourceOperations =
    new MergerEventSourceOperationsImpl(eventSourceOperations, objIdFactory, nodeAttrs, instantTxManager, mainTxManager, findNodes, currentRequest)
}

trait SessionDBConnectionMix extends DBConnectionMix {
  lazy val muxFactory = new MuxFactoryImpl
  lazy val mainTxManager =
    new SessionMainTxManagerImpl(lifeCycle, dbAppMix.mainDB, mainTx, preCommitCheckCheckAll, muxFactory)
  lazy val sessionEventSourceOperations =
    new SessionEventSourceOperationsImpl(
      eventSourceOperations, eventSourceAttrs, nodeAttrs, findAttrs, handlerLists, instantTxManager, mainTxManager, findNodes
    )()
}

/*
class MixedReadModelContext(attrInfoList: =>List[AttrInfo], rawIndex: RawIndex) {
  private lazy val lazyAttrInfoList = attrInfoList
  private lazy val attrCalcExecutor = new AttrCalcExecutor(lazyAttrInfoList)
  private lazy val attrCalcInfo = new AttrInfoRegistry(lazyAttrInfoList)

  /*private lazy val muxIndex =
    new MuxUnmergedIndex(new EmptyUnmergedIndex, rawIndex)*/
  private lazy val innerIndex =
    new MixedInnerIndex(2L, 3L, rawIndex, attrCalcInfo.indexed)
  private lazy val index =
    new RewritableTriggeringIndex(innerIndex.innerIndex, attrCalcExecutor)

  private lazy val preCommitCalcCollector = new PreCommitCalcCollectorImpl
  private lazy val sysAttrCalcContext =
    new SysAttrCalcContext(index, innerIndex.indexSearch, ThrowValidateFailReaction/*IgnoreValidateFailReaction*/)
  private lazy val sysPreCommitCheckContext =
    new SysPreCommitCheckContext(index, innerIndex.indexSearch, preCommitCalcCollector, ThrowValidateFailReaction)
  lazy val labelIndexAttrInfoList = new LabelIndexAttrInfoList(SearchAttrInfoFactoryImpl)
  lazy val labelPropIndexAttrInfoList =
    new LabelPropIndexAttrInfoList(sysAttrCalcContext, SearchAttrInfoFactoryImpl)
  lazy val relSideAttrInfoList =
    new RelSideAttrInfoList(sysAttrCalcContext, sysPreCommitCheckContext, SearchAttrInfoFactoryImpl)
  lazy val mandatoryPreCommitCheckList =
    new MandatoryPreCommitCheckList(sysPreCommitCheckContext)
  lazy val uniqueAttrCalcList = new UniqueAttrCalcList(sysAttrCalcContext)
  lazy val deleteAttrCalcList = new DeleteAttrCalcList(sysAttrCalcContext)
}
*/

/*
connection
longTx
invalidate +
frame/dbTx
event
 */

/*
in:
MuxUnmergedTx
NonEmptyUnmergedTx

?:
Replay
AllOriginalFactExtractor
BlockIterator
*/
