package ee.cone.base.db

import java.util.UUID
import java.util.concurrent.ExecutorService

import ee.cone.base.connection_api._

trait DBAppMix extends AppMixBase {
  def mainDB: DBEnv[MainEnvKey]
  def instantDB: DBEnv[InstantEnvKey]
  def createMergerConnection: LifeCycle=>CoMixBase
  lazy val mergerCurrentRequest = new CurrentRequest(None)
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

  // L0
  lazy val rawFactConverter = new RawFactConverterImpl
  lazy val rawSearchConverter = new RawSearchConverterImpl
  lazy val attrIdExtractor = new AttrIdExtractor
  lazy val objIdExtractor = new ObjIdExtractor(rawFactConverter)
  // L1
  lazy val rawVisitor = new RawVisitorImpl
  // L2
  lazy val noObj = new NoObjImpl(handlerLists)

  lazy val asDefined = new AttrValueType[Boolean]
  lazy val attrFactory = new AttrFactoryImpl(asDefined)()
  lazy val dbWrapType = new DBWrapType
  lazy val asDBNode = new AttrValueType[DBNode]
  lazy val nodeAttrs = new NodeAttrsImpl(attrFactory, asDefined, asDBNode)()
  lazy val nodeFactory = new NodeFactoryImpl(nodeAttrs,noObj,dbWrapType)()
  lazy val factIndex =
    new FactIndexImpl(rawFactConverter, attrIdExtractor, rawVisitor, handlerLists, nodeAttrs, attrFactory, dbWrapType)


  lazy val searchIndex =
    new SearchIndexImpl(handlerLists, rawSearchConverter, objIdExtractor, rawVisitor, attrFactory, nodeAttrs)

  // L3
  lazy val asAttr = new AttrValueType[Attr[Boolean]]


  lazy val preCommitCheckCheckAll = new PreCommitCheckAllOfConnectionImpl(nodeAttrs)
  //lazy val listByDBNode =
  //  new ListByDBNodeImpl(factIndex,attrValueConverter)
  lazy val findNodes = new FindNodesImpl(sysAttrs, handlerLists, nodeAttrs, nodeFactory, attrFactory, factIndex)

  lazy val mandatory = new MandatoryImpl(attrFactory, preCommitCheckCheckAll)
  lazy val unique = new UniqueImpl(attrFactory, nodeAttrs, preCommitCheckCheckAll, searchIndex, findNodes)

  lazy val instantTx = new CurrentTxImpl[InstantEnvKey](dbAppMix.instantDB)
  lazy val mainTx = new CurrentTxImpl[MainEnvKey](dbAppMix.mainDB)

  lazy val asNode = new AttrValueType[Obj]
  lazy val asUUID = new AttrValueType[Option[UUID]]
  lazy val asString = new AttrValueType[String]

  lazy val labelFactory = new LabelFactoryImpl(attrFactory,asNode)
  lazy val sysAttrs = new SysAttrsImpl(attrFactory,labelFactory,searchIndex,asNode,asUUID,asString)()

  lazy val uniqueNodes = new UniqueNodesImpl(nodeAttrs, nodeFactory, sysAttrs, findNodes, mandatory, unique, factIndex)


  lazy val instantTxManager =
    new DefaultTxManagerImpl[InstantEnvKey](lifeCycle, dbAppMix.instantDB, instantTx, preCommitCheckCheckAll)

  // Sys
  lazy val eventSourceAttrs =
    new EventSourceAttrsImpl(attrFactory,labelFactory,asDefined,asNode,asAttr,asUUID,asString)()
  lazy val eventSourceOperations =
    new EventSourceOperationsImpl(eventSourceAttrs,nodeAttrs,sysAttrs,factIndex,handlerLists,findNodes,uniqueNodes,instantTx,mainTx,searchIndex,mandatory)

  lazy val alienAccessAttrs = new AlienAccessAttrs(attrFactory, asDefined)()
  lazy val alienCanChange = new AlienCanChange(alienAccessAttrs,attrFactory,handlerLists,uniqueNodes,mainTx,factIndex)()

  override def handlers =
    nodeFactory.handlers :::
      findNodes.handlers :::
      uniqueNodes.handlers :::
      eventSourceOperations.handlers :::
      alienCanChange.handlers :::
      new DefinedValueConverter(asDefined, InnerRawValueConverterImpl).handlers :::
      new AttrValueConverter(asAttr,InnerRawValueConverterImpl,attrFactory,asDefined).handlers :::
      new MainNodeValueConverter(asNode,InnerRawValueConverterImpl,nodeFactory,nodeAttrs,mainTx,instantTx).handlers :::
      new UUIDValueConverter(asUUID,InnerRawValueConverterImpl).handlers :::
      new StringValueConverter(asString,InnerRawValueConverterImpl).handlers :::
      super.handlers
}

trait MergerDBConnectionMix extends DBConnectionMix {
  lazy val currentRequest = dbAppMix.mergerCurrentRequest
  lazy val mainTxManager =
    new DefaultTxManagerImpl[MainEnvKey](lifeCycle, dbAppMix.mainDB, mainTx, preCommitCheckCheckAll)
  override def handlers =
    new MergerEventSourceOperationsImpl(eventSourceOperations, nodeAttrs, instantTxManager, mainTxManager, uniqueNodes, currentRequest).handlers :::
    super.handlers
}

trait SessionDBConnectionMix extends DBConnectionMix {
  lazy val muxFactory = new MuxFactoryImpl
  lazy val mainTxManager =
    new SessionMainTxManagerImpl(lifeCycle, dbAppMix.mainDB, mainTx, preCommitCheckCheckAll, muxFactory)
  override def handlers =
    new SessionEventSourceOperationsImpl(
      eventSourceOperations, eventSourceAttrs, sysAttrs, instantTxManager, mainTxManager, findNodes, uniqueNodes
    ).handlers :::
    super.handlers
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
