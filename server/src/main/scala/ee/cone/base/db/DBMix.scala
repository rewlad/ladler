package ee.cone.base.db

import ee.cone.base.connection_api._
import ee.cone.base.db.Types.ObjId

trait DBEnv extends AppComponent with CanStart

trait DBAppMix extends MixBase[AppComponent] {
  def mainDB: DBEnv
  def instantDB: DBEnv
  override def createComponents() = mainDB :: instantDB :: super.createComponents()
}

trait DBConnectionMix extends MixBase[ConnectionComponent] with Runnable {
  def dbAppMix: DBAppMix

  lazy val rawFactConverter = new RawFactConverterImpl
  lazy val rawSearchConverter = new RawSearchConverterImpl
  lazy val attrIdExtractor = new AttrIdExtractor
  lazy val objIdExtractor = new ObjIdExtractor
  lazy val attrIdRawVisitor = new RawVisitorImpl(attrIdExtractor)
  lazy val objIdRawVisitor = new RawVisitorImpl(objIdExtractor)
  lazy val attrCalcLists = new AttrCalcLists(components)
  lazy val searchAttrCalcCheck = new SearchAttrCalcCheck(components)

  lazy val mainFactIndex =
    new FactIndexImpl(true, rawFactConverter, attrIdRawVisitor, attrCalcLists)
  lazy val instantFactIndex =
    new FactIndexImpl(false, rawFactConverter, attrIdRawVisitor, attrCalcLists)
  lazy val mainSearchIndex =
    new SearchIndexImpl(rawSearchConverter, objIdRawVisitor, mainFactIndex, searchAttrCalcCheck)
  lazy val instantSearchIndex =
    new SearchIndexImpl(rawSearchConverter, objIdRawVisitor, instantFactIndex, searchAttrCalcCheck)


  override def createComponents() = /*all prop.components*/ super.createComponents()
}





trait DBTxMix {
  //new FactRawIndexRegistration()
  //new SearchRawIndexRegistration
  //new SrcObjIdRegistration()
}



/*
class MixedInnerIndex(
  factHead: Long, indexHead: Long, rawIndex: RawIndex/*T*/, indexed: Long=>Boolean
){
  lazy val rawFactConverter = new RawFactConverterImpl(factHead, 0/*T:valueSrcId*/)
  private lazy val rawIndexConverter = new RawIndexConverterImpl(indexHead)
  lazy val innerIndex =
    new InnerIndex(rawFactConverter, rawIndexConverter, rawIndex, indexed)
  /*lazy val indexSearch =
    new IndexSearchImpl(rawFactConverter, rawIndexConverter, RawKeyMatcherImpl, rawIndex)*/
}

class MixedUnmergedEventDBContext(rawIndex: RawIndex, indexed: Long=>Boolean){
  private lazy val eventInnerIndex =
    new MixedInnerIndex(0L, 1L, rawIndex, indexed)
  lazy val eventIndex =
    new AppendOnlyIndex(eventInnerIndex.innerIndex)

}

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
