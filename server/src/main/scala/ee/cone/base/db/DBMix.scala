package ee.cone.base.db

import java.util.concurrent.ExecutorService

import ee.cone.base.connection_api._

trait DBAppMix extends AppMixBase {
  def mainDB: DBEnv
  def instantDB: DBEnv
  def createMergerConnection: LifeCycle=>CoMixBase
  lazy val mergerCurrentRequest = new CurrentRequest(None)
  override def toStart =
    mainDB :: instantDB :: new Merger(executionManager,createMergerConnection) :: super.toStart
}

trait DBConnectionMix extends CoMixBase {
  def dbAppMix: DBAppMix
  def lifeCycle: LifeCycle

  lazy val rawFactConverter = new RawFactConverterImpl
  lazy val rawSearchConverter = new RawSearchConverterImpl
  lazy val attrIdExtractor = new AttrIdExtractor
  lazy val objIdExtractor = new ObjIdExtractor
  lazy val attrIdRawVisitor = new RawVisitorImpl(attrIdExtractor)
  lazy val objIdRawVisitor = new RawVisitorImpl(objIdExtractor)
  lazy val factIndex =
    new FactIndexImpl(rawFactConverter, attrIdRawVisitor, handlerLists)
  lazy val definedValueConverter =
    new DefinedValueConverter(InnerRawValueConverterImpl)
  lazy val attrFactory =
    new AttrFactoryImpl(definedValueConverter, factIndex)
  lazy val searchIndex =
    new SearchIndexImpl(rawSearchConverter, objIdRawVisitor, attrFactory)

  lazy val preCommitCheckCheckAll = new PreCommitCheckAllOfConnectionImpl
  lazy val listByDBNode =
    new ListByDBNodeImpl(factIndex,attrFactory,definedValueConverter)
  lazy val mandatory = new MandatoryImpl(preCommitCheckCheckAll)
  lazy val nodeFactory = new NodeFactoryImpl

  lazy val instantTx = new CurrentTxImpl[InstantEnvKey]
  lazy val mainTx = new CurrentTxImpl[MainEnvKey]

  lazy val attrValueConverter = new AttrValueConverter(InnerRawValueConverterImpl,attrFactory,definedValueConverter)
  lazy val nodeValueConverter = new NodeValueConverter(InnerRawValueConverterImpl,nodeFactory,instantTx,mainTx)()
  lazy val uuidValueConverter = new UUIDValueConverter(InnerRawValueConverterImpl)
  lazy val stringValueConverter = new StringValueConverter(InnerRawValueConverterImpl)

  lazy val sysAttrs = new SysAttrs(attrFactory,nodeValueConverter)()
  lazy val allNodes = new DBNodesImpl(handlerLists, nodeFactory, sysAttrs)

  lazy val instantTxManager =
    new DefaultTxManagerImpl[InstantEnvKey](lifeCycle, dbAppMix.instantDB, instantTx, preCommitCheckCheckAll)


  lazy val eventSourceAttrs =
    new EventSourceAttrsImpl(attrFactory,searchIndex,nodeValueConverter,attrValueConverter,uuidValueConverter,stringValueConverter,mandatory)()()
  lazy val eventSourceOperations =
    new EventSourceOperationsImpl(eventSourceAttrs,factIndex,handlerLists,allNodes,nodeFactory,instantTx,mainTx)

  override def handlers = eventSourceAttrs.handlers ::: super.handlers
}

trait MergerDBConnectionMix extends DBConnectionMix {
  lazy val currentRequest = dbAppMix.mergerCurrentRequest
  lazy val mainTxManager =
    new DefaultTxManagerImpl[MainEnvKey](lifeCycle, dbAppMix.mainDB, mainTx, preCommitCheckCheckAll)
  override def handlers =
    new MergerEventSourceOperationsImpl(eventSourceOperations, eventSourceAttrs, instantTxManager, mainTxManager, nodeFactory, currentRequest).handlers :::
    super.handlers
}

trait SessionDBConnectionMix extends DBConnectionMix {
  lazy val muxFactory = new MuxFactoryImpl
  lazy val mainTxManager =
    new SessionMainTxManagerImpl(lifeCycle, dbAppMix.mainDB, mainTx, preCommitCheckCheckAll, muxFactory)
  lazy val sessionEventSourceOperations =
    new SessionEventSourceOperationsImpl(eventSourceOperations, eventSourceAttrs, instantTxManager, mainTxManager, nodeFactory, allNodes)
  override def handlers = sessionEventSourceOperations.handlers ::: super.handlers
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
