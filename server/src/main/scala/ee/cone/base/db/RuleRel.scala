package ee.cone.base.db

import java.util.UUID

import ee.cone.base.util.Never

// fail'll probably do nothing in case of outdated rel type

class RelSideAttrInfoList(
  sysAttrCalcContext: SysAttrCalcContext,
  sysPreCommitCheckContext: SysPreCommitCheckContext,
  createSearchAttrInfo: SearchAttrInfoFactory
) {
  def apply(
    relSideAttrInfo: List[NameAttrInfo], typeAttrId: AttrId,
    relTypeAttrInfo: List[NameAttrInfo]
  ): List[AttrInfo] = relSideAttrInfo.flatMap{ propInfo ⇒
    val relComposedAttrInfo: List[SearchAttrInfo] =
      relTypeAttrInfo.map(i⇒createSearchAttrInfo(Some(i), Some(propInfo)))
    val relTypeAttrIdToComposedAttrId =
      relComposedAttrInfo.map{ i ⇒ i.labelAttr.toString → i.attrId }.toMap
    val indexedAttrIds = relTypeAttrIdToComposedAttrId.values.toSet
    val calc = TypeIndexAttrCalc(
      typeAttrId, propInfo.attrId,
      relTypeAttrIdToComposedAttrId, indexedAttrIds
      //_IgnoreValidateFailReaction()
    )(sysAttrCalcContext)
    val integrity = createRefIntegrityPreCommitCheckList(
      typeAttrId, propInfo.attrId/*, T h rowValidateFailReaction()*/
    )
    calc :: createSearchAttrInfo(None, Some(propInfo)) :: relComposedAttrInfo ::: integrity

  }
  private def createRefIntegrityPreCommitCheckList(typeAttrId: AttrId, toAttrId: AttrId): List[AttrCalc] =
    TypeRefIntegrityPreCommitCheck(typeAttrId, toAttrId)(sysPreCommitCheckContext) ::
    SideRefIntegrityPreCommitCheck(typeAttrId, toAttrId)(sysPreCommitCheckContext) :: Nil
}

case class TypeIndexAttrCalc(
  typeAttrId: AttrId, propAttrId: AttrId,
  relTypeIdToAttrId: Map[String,AttrId], indexedAttrIds: Set[AttrId]
)
  (context: SysAttrCalcContext)
  extends AttrCalc
{
  import context._
  def version = UUID.fromString("a6e93a68-1df8-4ee7-8b3f-1cb5ae768c42")
  def affectedBy = typeAttrId :: propAttrId :: Nil
  def recalculate(objId: ObjId) = {
    listAttrIdsByObjId(objId)
      .foreach(attrId => if(indexedAttrIds(attrId)) db(objId, attrId) = DBRemoved)
    (db(objId, typeAttrId), db(objId, propAttrId)) match {
      case (_,DBRemoved) | (DBRemoved,_) => ()
      case (DBStringValue(typeIdStr),value) =>
        val attrIdOpt: Option[AttrId] = relTypeIdToAttrId.get(typeIdStr)
        if(attrIdOpt.isEmpty) fail(objId, "never here")
        else db(objId, attrIdOpt.get) = value
      case _ => Never()
    }
  }
}

abstract class RefIntegrityPreCommitCheck(context: SysPreCommitCheckContext)
  extends AttrCalc
{
  import context._
  private def dbHas(objId: ObjId, attrId: AttrId) = db(objId, attrId) != DBRemoved
  protected def typeAttrId: AttrId
  protected def toAttrId: AttrId
  protected def checkAll(objIds: Seq[ObjId]): Unit
  protected def check(objId: ObjId) = db(objId, toAttrId) match {
    case DBRemoved ⇒ ()
    case DBLongValue(toObjId) if dbHas(toObjId, typeAttrId) ⇒ ()
    case v => fail(objId, s"attr $toAttrId should refer to valid object, but $v found")
  }
  def recalculate(objId: ObjId) = add(objId)
  private lazy val add = preCommitCalcCollector(checkAll)
}

//toAttrId must be indexed
case class TypeRefIntegrityPreCommitCheck(typeAttrId: AttrId, toAttrId: AttrId)
  (context: SysPreCommitCheckContext)
  extends RefIntegrityPreCommitCheck(context)
{
  import context._
  def version = UUID.fromString("b2232ecf-734c-4cfa-a88f-78b066a01cd3")
  def affectedBy = typeAttrId :: Nil
  private def referredBy(objId: ObjId): List[ObjId] =
    listObjIdsByValue(toAttrId, new DBLongValue(objId))
  protected def checkAll(objIds: Seq[ObjId]) =
    objIds.flatMap(referredBy).foreach(check)
}

case class SideRefIntegrityPreCommitCheck(typeAttrId: AttrId, toAttrId: AttrId)
  (context: SysPreCommitCheckContext)
  extends RefIntegrityPreCommitCheck(context)
{
  def version = UUID.fromString("677f2fdc-b56e-4cf8-973f-db148ee3f0c4")
  def affectedBy = toAttrId :: Nil
  protected def checkAll(objIds: Seq[ObjId]) = objIds.foreach(check)
}
