package ee.cone.base.db

import ee.cone.base.util.Never

object AttrIdCompose { // do not use for original facts
  def hexSz = 4
  def idSz = 4 * hexSz
  def apply(a: Long, b: Long): Long =
    LongFits(a,idSz, isUnsigned = true, idSz) |
      LongFits(b,idSz, isUnsigned = true, 0)
}

/*NoGEN*/ trait BaseNameAttrInfo extends AttrInfo {
  def attrId: Long
  def nameOpt: Option[String]
}
case class NoNameAttrInfo(attrId: Long) extends BaseNameAttrInfo {
  def nameOpt = None
}
case class NameAttrInfo(attrId: Long, name: String) extends BaseNameAttrInfo {
  lazy val nameOpt = Some(name)
}
case class SearchAttrInfo(
  labelOpt: Option[NameAttrInfo], propOpt: Option[NameAttrInfo]
) extends BaseNameAttrInfo with IndexAttrInfo {
  def attrId = composedInfo.attrId
  def nameOpt = composedInfo.nameOpt
  private lazy val composedInfo: BaseNameAttrInfo = (labelOpt,propOpt) match {
    case (None,Some(info)) ⇒ info
    case (Some(info),None) ⇒ info
    case (Some(NameAttrInfo(labelAttrId,_)),Some(NameAttrInfo(propAttrId,_))) ⇒
      NoNameAttrInfo(AttrIdCompose(labelAttrId,propAttrId))
    case (None,None) ⇒ Never()
  }
  lazy val keyForValue: String = propOpt.orElse(labelOpt).flatMap(_.nameOpt).get
}

object RelSideAttrInfoList {
  def apply(
    relSideAttrInfo: List[NameAttrInfo], typeAttrId: Long,
    relTypeAttrInfo: List[NameAttrInfo]
  ): List[AttrInfo] = relSideAttrInfo.flatMap{ propInfo ⇒
    val relComposedAttrInfo =
      relTypeAttrInfo.map(i⇒SearchAttrInfo(Some(i), Some(propInfo)))
    val relTypeAttrIdToComposedAttrId =
      relComposedAttrInfo.map{ i ⇒ i.labelOpt.get.attrId.toString → i.attrId }.toMap
    val indexedAttrIds = relTypeAttrIdToComposedAttrId.values.toSet
    val calc = TypeIndexAttrCalc(
      typeAttrId, propInfo.attrId,
      relTypeAttrIdToComposedAttrId, indexedAttrIds,
      IgnoreValidateFailReaction()
    )
    calc :: SearchAttrInfo(None, Some(propInfo)) :: relComposedAttrInfo :::
      RefIntegrityPreCommitCheckList(typeAttrId, propInfo.attrId, ThrowValidateFailReaction())
  }
}
object LabelPropIndexAttrInfoList {
  def apply(labelInfo: NameAttrInfo, propInfo: NameAttrInfo): List[AttrInfo] = {
    val searchInfo = SearchAttrInfo(Some(labelInfo), Some(propInfo))
    val calc = LabelIndexAttrCalc(labelInfo.attrId, propInfo.attrId, searchInfo.attrId)
    calc :: searchInfo :: Nil
  }
}
object LabelIndexAttrInfoList {
  def apply(labelInfo: NameAttrInfo): List[AttrInfo] =
    SearchAttrInfo(Some(labelInfo), None) :: Nil
}
