package ee.cone.base.db

import java.util.UUID

class LabelIndexAttrInfoList(createSearchAttrInfo: SearchAttrInfoFactory) {
  def apply(labelInfo: NameAttrInfo): List[AttrInfo] =
    createSearchAttrInfo(Some(labelInfo), None) :: Nil
}

class LabelPropIndexAttrInfoList(
  createSearchAttrInfo: SearchAttrInfoFactory
) {
  def apply(labelInfo: NameAttrInfo, propInfo: NameAttrInfo): List[AttrInfo] = {
    val searchInfo = createSearchAttrInfo(Some(labelInfo), Some(propInfo))
    val calc = LabelIndexAttrCalc(
      labelInfo.attr, propInfo.attr, searchInfo.attr
    )
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
