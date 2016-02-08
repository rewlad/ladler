package ee.cone.base.db

import java.util.UUID

class LabelIndexAttrInfoList(createSearchAttrInfo: SearchAttrInfoFactory) {
  def apply(labelInfo: NameAttrInfo): List[AttrInfo] =
    createSearchAttrInfo(Some(labelInfo), None) :: Nil
}

class LabelPropIndexAttrInfoList(
  sysAttrCalcContext: SysAttrCalcContext,
  createSearchAttrInfo: SearchAttrInfoFactory
) {
  def apply(labelInfo: NameAttrInfo, propInfo: NameAttrInfo): List[AttrInfo] = {
    val searchInfo = createSearchAttrInfo(Some(labelInfo), Some(propInfo))
    val calc = LabelIndexAttrCalc(
      labelInfo.attrId, propInfo.attrId, searchInfo.attrId
    )(sysAttrCalcContext)
    calc :: searchInfo :: Nil
  }
}

case class LabelIndexAttrCalc(labelAttrId: AttrId, propAttrId: AttrId, indexedAttrId: AttrId)
  (context: SysAttrCalcContext)
  extends AttrCalc
{
  import context._
  private def dbHas(objId: ObjId, attrId: AttrId) = db(objId, attrId) != DBRemoved
  def version = UUID.fromString("1afd3999-46ac-4da3-84a6-17d978f7e032")
  def affectedByAttrIds = labelAttrId :: propAttrId :: Nil
  def recalculate(objId: ObjId) = db(objId, indexedAttrId) =
    if(!dbHas(objId, labelAttrId)) DBRemoved else db(objId, propAttrId)
}
