package ee.cone.base.db

// LabelIndexAttrInfoList -- just indexed=true in labelAttr

class LabelPropIndexAttrInfoList(
  createSearchAttrInfo: IndexComposer
) {
  def apply(labelInfo: RuledIndex, propInfo: RuledIndex): List[AttrInfo] = {
    val searchInfo = createSearchAttrInfo(labelInfo, propInfo)
    val calc = LabelIndexAttrCalc(labelInfo, propInfo, searchInfo)
    calc :: searchInfo :: Nil
  }
}

case class LabelIndexAttrCalc(
  labelAttr: RuledIndex, propAttr: RuledIndex, indexedAttr: RuledIndex,
  version: String = "1afd3999-46ac-4da3-84a6-17d978f7e032"
) extends AttrCalc {
  def affectedBy = labelAttr :: propAttr :: Nil
  def recalculate(objId: ObjId) =
    indexedAttr(objId) = if(labelAttr(objId)==DBRemoved) DBRemoved else propAttr(objId)
}
