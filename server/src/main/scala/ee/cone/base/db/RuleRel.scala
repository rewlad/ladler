package ee.cone.base.db

import java.util.UUID

import ee.cone.base.util.Never

// fail'll probably do nothing in case of outdated rel type

class RelSideAttrInfoList(
  preCommitCheck: PreCommitCheck=>AttrCalc,
  createSearchAttrInfo: IndexComposer,
  attrs: SearchByObjId
) {
  def apply(
    relSideAttrInfo: List[SearchByValue[Option[ObjId]]], typeAttr: RuledIndex,
    relTypeAttrInfo: List[RuledIndex]
  ): List[AttrInfo] = relSideAttrInfo.flatMap{ searchToAttr ⇒
    val propInfo = searchToAttr.direct.ruled
    val relComposedAttrInfo: List[(RuledIndex,RuledIndex)] =
      relTypeAttrInfo.map(labelAttr⇒(labelAttr,createSearchAttrInfo(labelAttr, propInfo)))
    val relTypeAttrIdToComposedAttr =
      relComposedAttrInfo.map{ case(l,i) ⇒ l.attrId.toString → i }.toMap
    val indexedAttrIds = relTypeAttrIdToComposedAttr.values.toSet
    val calc = TypeIndexAttrCalc(typeAttr, propInfo, attrs)(relTypeAttrIdToComposedAttr, indexedAttrIds)
    val integrity = createRefIntegrityPreCommitCheckList(typeAttr, searchToAttr)
    calc :: propInfo :: relComposedAttrInfo.map(_._2) ::: integrity
  }
  private def createRefIntegrityPreCommitCheckList(
    typeAttr: RuledIndex,
    searchToAttrId: SearchByValue[Option[ObjId]]
  ): List[AttrCalc] =
    preCommitCheck(TypeRefIntegrityPreCommitCheck(typeAttr, searchToAttrId)) ::
      preCommitCheck(SideRefIntegrityPreCommitCheck(typeAttr, searchToAttrId.direct)) :: Nil
}

case class TypeIndexAttrCalc(
  typeAttr: RuledIndex, propAttr: RuledIndex, attrs: SearchByObjId,
  version: String = "a6e93a68-1df8-4ee7-8b3f-1cb5ae768c42"
)(
  relTypeIdToAttr: String=>RuledIndex, indexed: RuledIndex=>Boolean // relTypeIdToAttr.getOrElse(typeIdStr, throw new Exception(s"bad rel type $typeIdStr of $objId never here"))
) extends AttrCalc {
  def affectedBy = typeAttr :: propAttr :: Nil
  def beforeUpdate(objId: ObjId) = ()
  def afterUpdate(objId: ObjId) = {
    attrs(objId).foreach(attr => if(indexed(attr)) attr(objId) = DBRemoved)
    (typeAttr(objId), propAttr(objId)) match {
      case (_,DBRemoved) | (DBRemoved,_) => ()
      case (DBStringValue(typeIdStr),value) => relTypeIdToAttr(typeIdStr)(objId) = value
      case _ => Never()
    }
  }
}

abstract class RefIntegrityPreCommitCheck extends PreCommitCheck {
  protected def typeAttr: RuledIndex
  protected def toAttr: RuledIndexAdapter[Option[ObjId]]
  protected def check(objId: ObjId): Option[ValidationFailure] = toAttr(objId) match {
    case None ⇒ None
    case Some(toObjId) if typeAttr(toObjId) != DBRemoved ⇒ None
    case v => Some(ValidationFailure(this,objId)) //, s"attr $toAttrId should refer to valid object, but $v found")
  }
}

//toAttrId must be indexed
case class TypeRefIntegrityPreCommitCheck(
  typeAttr: RuledIndex,
  searchToAttr: SearchByValue[Option[ObjId]],
  version: String = "b2232ecf-734c-4cfa-a88f-78b066a01cd3"
) extends RefIntegrityPreCommitCheck {
  protected def toAttr = searchToAttr.direct
  def affectedBy = typeAttr :: Nil
  def check(objIds: Seq[ObjId]) =
    objIds.flatMap(objId => searchToAttr(Some(objId))).flatMap(check)
}

case class SideRefIntegrityPreCommitCheck(
  typeAttr: RuledIndex, toAttr: RuledIndexAdapter[Option[ObjId]],
  version: String = "677f2fdc-b56e-4cf8-973f-db148ee3f0c4"
) extends RefIntegrityPreCommitCheck {
  def affectedBy = toAttr.ruled :: Nil
  def check(objIds: Seq[ObjId]) = objIds.flatMap(check)
}
