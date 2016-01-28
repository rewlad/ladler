package ee.cone.base.db

class DefaultMixedTx(rawTx: RawTx, attrInfoList: List[AttrInfo]) {
  lazy val attrCalcInfo = new AttrCalcInfo(attrInfoList)

  lazy val indexingTx = new IndexingTxImpl(RawFactConverterImpl, RawIndexConverterImpl, rawTx, attrCalcInfo)
  lazy val indexSearch = new IndexSearchImpl(RawFactConverterImpl, RawIndexConverterImpl, RawKeyMatcherImpl, rawTx)
  lazy val preCommitCalcCollector = new PreCommitCalcCollectorImpl

  lazy val sysAttrCalcContext = new SysAttrCalcContext(indexingTx, indexSearch, ThrowValidateFailReaction/*IgnoreValidateFailReaction*/)
  lazy val sysPreCommitCheckContext = new SysPreCommitCheckContext(indexingTx, indexSearch, preCommitCalcCollector, ThrowValidateFailReaction)

  lazy val labelIndexAttrInfoList = new LabelIndexAttrInfoList(SearchAttrInfoFactoryImpl)
  lazy val labelPropIndexAttrInfoList = new LabelPropIndexAttrInfoList(sysAttrCalcContext, SearchAttrInfoFactoryImpl)
  lazy val relSideAttrInfoList = new RelSideAttrInfoList(sysAttrCalcContext, sysPreCommitCheckContext, SearchAttrInfoFactoryImpl)
  lazy val mandatoryPreCommitCheckList = new MandatoryPreCommitCheckList(sysPreCommitCheckContext)
  lazy val uniqueAttrCalcList = new UniqueAttrCalcList(sysAttrCalcContext)
  lazy val deleteAttrCalcList = new DeleteAttrCalcList(sysAttrCalcContext)

}

/*
in:
MuxUnmergedTx
NonEmptyUnmergedTx

?:
Replay
AllOriginalFactExtractor
BlockIterator
*/
