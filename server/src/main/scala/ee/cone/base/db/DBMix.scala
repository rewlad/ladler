package ee.cone.base.db

class DefaultMixedTx(rawTx: RawTx/*T*/, attrInfoList: List[AttrInfo]) {
  lazy val lazyAttrInfoList = attrInfoList
  //lazy val attrCalcExecutor = new AttrCalcExecutor(lazyAttrInfoList)
  lazy val attrCalcInfo = new AttrInfoRegistry(lazyAttrInfoList)

  private lazy val valueSrcId = 0/*T*/
  private lazy val rawFactConverter = new RawFactConverterImpl(0L, valueSrcId)
  private lazy val rawIndexConverter = new RawIndexConverterImpl(1L)
  private lazy val innerIndex =
    new InnerIndex(rawFactConverter, rawIndexConverter, rawTx, attrCalcInfo.indexed)
  private lazy val index = new AppendOnlyIndex(innerIndex)
  private lazy val indexSearch =
    new IndexSearchImpl(rawFactConverter, rawIndexConverter, RawKeyMatcherImpl, rawTx)




  private lazy val preCommitCalcCollector = new PreCommitCalcCollectorImpl
  private lazy val sysAttrCalcContext =
    new SysAttrCalcContext(index, indexSearch, ThrowValidateFailReaction/*IgnoreValidateFailReaction*/)
  private lazy val sysPreCommitCheckContext =
    new SysPreCommitCheckContext(index, indexSearch, preCommitCalcCollector, ThrowValidateFailReaction)
  lazy val labelIndexAttrInfoList =
    new LabelIndexAttrInfoList(SearchAttrInfoFactoryImpl)
  lazy val labelPropIndexAttrInfoList =
    new LabelPropIndexAttrInfoList(sysAttrCalcContext, SearchAttrInfoFactoryImpl)
  lazy val relSideAttrInfoList =
    new RelSideAttrInfoList(sysAttrCalcContext, sysPreCommitCheckContext, SearchAttrInfoFactoryImpl)
  lazy val mandatoryPreCommitCheckList =
    new MandatoryPreCommitCheckList(sysPreCommitCheckContext)
  lazy val uniqueAttrCalcList = new UniqueAttrCalcList(sysAttrCalcContext)
  lazy val deleteAttrCalcList = new DeleteAttrCalcList(sysAttrCalcContext)
}

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
