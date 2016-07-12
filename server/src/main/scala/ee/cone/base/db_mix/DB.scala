package ee.cone.base.db_mix

import ee.cone.base.db_impl._
import ee.cone.base.db.{InstantEnvKey, DBEnv, MainEnvKey}
import ee.cone.base.connection_api._


trait DBAppMix extends AppMixBase {
  def mainDB: DBEnv[MainEnvKey]
  def instantDB: DBEnv[InstantEnvKey]
  def createMergerConnection: LifeCycle=>CoMixBase
  lazy val mergerCurrentRequest = new CurrentRequest(NoObjId)
  override def toStart =
    new Merger(executionManager,createMergerConnection) :: super.toStart
  lazy val unsignedBytesOrdering = new UnsignedBytesOrdering
}

trait InstantInMemoryDBAppMix extends DBAppMix {
  lazy val instantDB = new InMemoryEnv[InstantEnvKey](0L,unsignedBytesOrdering)
}

trait MainInMemoryDBAppMix extends DBAppMix {
  lazy val mainDB = new InMemoryEnv[MainEnvKey](1L,unsignedBytesOrdering)
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

  lazy val findAttrs = new FindAttrsImpl(attrFactory,asDefined)()
  lazy val findNodes = new FindNodesImpl(findAttrs, handlerLists, nodeAttrs, noObj, attrFactory, objIdFactory, dbObjIdValueConverter, dbWrapType)()

  lazy val preCommitCheckCheckAll = new PreCommitCheckAllOfConnectionImpl(txSelector)
  lazy val mandatory = new MandatoryImpl(attrFactory, factIndex, preCommitCheckCheckAll)
  lazy val unique = new UniqueImpl(attrFactory, factIndex, txSelector, preCommitCheckCheckAll, searchIndex, findNodes)
  lazy val inheritAttrRule = new InheritAttrRuleImpl(attrFactory,findNodes,mainTx)

  lazy val basicValueTypes = new BasicValueTypesImpl(objIdFactory)()

  lazy val labelFactory = new LabelFactoryImpl(attrFactory,basicValueTypes)

  lazy val objOrderingFactory = new ObjOrderingFactoryImpl(handlerLists, attrFactory)

  lazy val instantTxManager =
    new DefaultTxManagerImpl[InstantEnvKey](lifeCycle, dbAppMix.instantDB, instantTx, preCommitCheckCheckAll)

  // Sys
  lazy val eventSourceAttrs =
    new EventSourceAttrsImpl(objIdFactory,attrFactory,labelFactory,asDBObjId,basicValueTypes)()
  lazy val eventSourceOperations =
    new EventSourceOperationsImpl(eventSourceAttrs,nodeAttrs,findAttrs,factIndex,handlerLists,findNodes,instantTx,mainTx,searchIndex,mandatory)()

  lazy val transient = new TransientImpl(handlerLists, attrFactory, dbWrapType)

  lazy val alienAttributes = new AlienAttributesImpl(objIdFactory, attrFactory, basicValueTypes)()
  lazy val alienWrapType = new AlienWrapType
  lazy val demandedWrapType = new DemandedWrapType
  lazy val alien = new AlienImpl(alienAttributes,nodeAttrs,attrFactory,handlerLists,findNodes,mainTx,factIndex,alienWrapType,demandedWrapType,objIdFactory,basicValueTypes,transient)

  lazy val objOrderingForAttrValueTypes = new ObjOrderingForAttrValueTypes(handlerLists, objOrderingFactory, basicValueTypes)

  // converters
  lazy val definedValueConverter = new DefinedValueConverter(asDefined, rawConverter)
  lazy val booleanValueConverter = new BooleanValueConverter(basicValueTypes, rawConverter)
  lazy val dbObjValueConverter = new DBObjValueConverter(basicValueTypes,dbObjIdValueConverter,findNodes,nodeAttrs)
  lazy val uuidValueConverter = new UUIDValueConverter(basicValueTypes,rawConverter)
  lazy val stringValueConverter = new StringValueConverter(basicValueTypes,rawConverter)
  lazy val bigDecimalValueConverter = new BigDecimalValueConverter(basicValueTypes,rawConverter)
  lazy val zoneIds = new ZoneIdsImpl
  lazy val instantValueConverter = new InstantValueConverter(basicValueTypes,rawConverter,zoneIds)
  lazy val durationValueConverter = new DurationValueConverter(basicValueTypes,rawConverter)
  lazy val localTimeValueConverter = new LocalTimeValueConverter(basicValueTypes,rawConverter)

  // VM
  lazy val uiStringAttributes = new UIStringAttributes(attrFactory, basicValueTypes)()
  lazy val uiStrings = new UIStringsImpl(uiStringAttributes, nodeAttrs, handlerLists, objIdFactory, attrFactory, factIndex, onUpdate, findNodes, asDBObjId, basicValueTypes)
  lazy val asObjValidation = AttrValueType[ObjValidation](objIdFactory.toObjId("f3ef68d8-60d3-4811-9db1-d187228feb89"))
  lazy val validationAttributes = new ValidationAttributesImpl(attrFactory,asObjValidation)()
  lazy val validationWrapType = new ValidationWrapType
  lazy val validationFactory = new ValidationFactoryImpl(handlerLists,validationAttributes,nodeAttrs,attrFactory,dbWrapType,validationWrapType)()

  lazy val lazyObjFactory = new LazyObjFactoryImpl(objIdFactory,attrFactory,findNodes,findAttrs,mainTx,alien)
  lazy val filterAttributes = new FilterAttributes(attrFactory, labelFactory, asDBObjId)()
  lazy val filterObjFactory = new FilterObjFactoryImpl(filterAttributes, nodeAttrs, handlerLists, factIndex, searchIndex, alien, lazyObjFactory)()

  lazy val orderingAttributes = new ItemListOrderingAttributes(attrFactory, asDBObjId, basicValueTypes)()
  lazy val itemListOrderingFactory = new ItemListOrderingFactoryImpl(orderingAttributes, attrFactory, alien, objOrderingFactory)

  lazy val asObjIdSet = AttrValueType[Set[ObjId]](objIdFactory.toObjId("ca3fd9c9-870f-4604-8fe2-a6ae98b37c29"))
  lazy val objIdSetValueConverter = new ObjIdSetValueConverter(asObjIdSet,rawConverter,objIdFactory)
  lazy val listedWrapType = new ListedWrapType
  lazy val editing = new EditingImpl(nodeAttrs,objIdFactory,alienAttributes,alien,dbWrapType)()
  lazy val itemListAttributes = new ObjSelectionAttributesImpl(attrFactory, asDBObjId, basicValueTypes, asObjIdSet)()

  lazy val indexedObjCollectionFactory = new IndexedObjCollectionFactoryImpl(attrFactory,findNodes,mainTx)

  lazy val objSelectionAttributes = new ObjSelectionAttributesImpl(attrFactory,asDBObjId,basicValueTypes,asObjIdSet)()
  lazy val objSelectionFactory = new ObjSelectionFactoryImpl(
    objSelectionAttributes,nodeAttrs,objIdFactory,findNodes,transient,alien,listedWrapType
  )
}

trait MergerDBConnectionMix extends DBConnectionMix {
  lazy val currentRequest = dbAppMix.mergerCurrentRequest
  lazy val mainTxManager =
    new DefaultTxManagerImpl[MainEnvKey](lifeCycle, dbAppMix.mainDB, mainTx, preCommitCheckCheckAll)
  lazy val mergerEventSourceOperations =
    new MergerEventSourceOperationsImpl(eventSourceOperations, objIdFactory, nodeAttrs, instantTxManager, mainTxManager, findNodes, currentRequest)
}

trait SessionDBConnectionMix extends DBConnectionMix {
  lazy val unsignedBytesOrdering = dbAppMix.unsignedBytesOrdering
  lazy val muxFactory = new MuxFactoryImpl(unsignedBytesOrdering)
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
