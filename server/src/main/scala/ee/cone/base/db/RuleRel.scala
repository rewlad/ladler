package ee.cone.base.db

import java.util.UUID

import ee.cone.base.util.Never

// fail'll probably do nothing in case of outdated rel type

class RelSideAttrInfoList(
  preCommitCheck: PreCommitCheck=>AttrCalc,
  createSearchAttrInfo: SearchAttrInfoFactory,
  attrs: AttrIndex[ObjId,List[RuledIndex]]
) {
  def apply(
    relSideAttrInfo: List[(NameAttrInfo,RuledIndexAdapter[Option[ObjId]],AttrIndex[Option[ObjId],List[ObjId]])], typeAttr: RuledIndex,
    relTypeAttrInfo: List[NameAttrInfo]
  ): List[AttrInfo] = relSideAttrInfo.flatMap{ case(propInfo,toAttr,searchToAttrId) ⇒
    val relComposedAttrInfo: List[SearchAttrInfo] =
      relTypeAttrInfo.map(i⇒createSearchAttrInfo(Some(i), Some(propInfo)))
    val relTypeAttrIdToComposedAttr =
      relComposedAttrInfo.map{ i ⇒ i.labelAttr.attrId.toString → i.attr }.toMap
    val indexedAttrIds = relTypeAttrIdToComposedAttr.values.toSet
    val calc = TypeIndexAttrCalc(typeAttr, propInfo.attr, attrs)(relTypeAttrIdToComposedAttr, indexedAttrIds)
    val integrity = createRefIntegrityPreCommitCheckList(
      typeAttr, toAttr /*propInfo.attr*/, searchToAttrId
    )
    calc :: createSearchAttrInfo(None, Some(propInfo)) :: relComposedAttrInfo ::: integrity
  }
  private def createRefIntegrityPreCommitCheckList(
    typeAttr: RuledIndex,
    toAttr: RuledIndexAdapter[Option[ObjId]],
    searchToAttrId: AttrIndex[Option[ObjId],List[ObjId]]
  ): List[AttrCalc] =
    preCommitCheck(TypeRefIntegrityPreCommitCheck(typeAttr, toAttr, searchToAttrId)) ::
      preCommitCheck(SideRefIntegrityPreCommitCheck(typeAttr, toAttr)) :: Nil
}

case class TypeIndexAttrCalc(
  typeAttr: RuledIndex, propAttr: RuledIndex, attrs: AttrIndex[ObjId,List[RuledIndex]],
  version: String = "a6e93a68-1df8-4ee7-8b3f-1cb5ae768c42"
)(
  relTypeIdToAttr: String=>RuledIndex, indexed: RuledIndex=>Boolean // relTypeIdToAttr.getOrElse(typeIdStr, throw new Exception(s"bad rel type $typeIdStr of $objId never here"))
) extends AttrCalc {
  def affectedBy = typeAttr :: propAttr :: Nil
  def recalculate(objId: ObjId) = {
    attrs(objId).foreach(attr => if(indexed(attr)) attr(objId) = DBRemoved)
    (typeAttr(objId), propAttr(objId)) match {
      case (_,DBRemoved) | (DBRemoved,_) => ()
      case (DBStringValue(typeIdStr),value) => relTypeIdToAttr(typeIdStr)(objId) = value
      case _ => Never()
    }
  }
}

class ObjIdValueConverter extends ValueConverter[Option[ObjId],DBValue] {
  override def apply(value: Option[ObjId]): DBValue = value match {
    case None => DBRemoved
    case Some(objId) => DBLongValue(objId.value)
  }
  override def apply(value: DBValue): Option[ObjId] = value match {
    case DBRemoved => None
    case DBLongValue(v) => Some(new ObjId(v))
    case _ => Never()
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
  toAttr: RuledIndexAdapter[Option[ObjId]],
  searchToAttrId: AttrIndex[Option[ObjId],List[ObjId]],
  version: String = "b2232ecf-734c-4cfa-a88f-78b066a01cd3"
) extends RefIntegrityPreCommitCheck {
  def affectedBy = typeAttr :: Nil
  def check(objIds: Seq[ObjId]) =
    objIds.flatMap(objId => searchToAttrId(Some(objId))).flatMap(check)
}

case class SideRefIntegrityPreCommitCheck(
  typeAttr: RuledIndex, toAttr: RuledIndexAdapter[Option[ObjId]],
  version: String = "677f2fdc-b56e-4cf8-973f-db148ee3f0c4"
) extends RefIntegrityPreCommitCheck {
  def affectedBy = toAttr.ruled :: Nil
  def check(objIds: Seq[ObjId]) = objIds.flatMap(check)
}
