package ee.cone.base.db

import java.util.UUID

import ee.cone.base.util.Never

class RelSideAttrInfoList(
  sysAttrCalcContext: SysAttrCalcContext,
  sysPreCommitCheckContext: SysPreCommitCheckContext,
  createSearchAttrInfo: SearchAttrInfoFactory
) {
  def apply(
    relSideAttrInfo: List[NameAttrInfo], typeAttrId: Long,
    relTypeAttrInfo: List[NameAttrInfo]
  ): List[AttrInfo] = relSideAttrInfo.flatMap{ propInfo ⇒
    val relComposedAttrInfo: List[SearchAttrInfo] =
      relTypeAttrInfo.map(i⇒createSearchAttrInfo(Some(i), Some(propInfo)))
    val relTypeAttrIdToComposedAttrId =
      relComposedAttrInfo.map{ i ⇒ i.labelAttrId.toString → i.attrId }.toMap
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
  private def createRefIntegrityPreCommitCheckList(typeAttrId: Long, toAttrId: Long): List[AttrCalc] =
    TypeRefIntegrityPreCommitCheck(typeAttrId, toAttrId)(sysPreCommitCheckContext) ::
    SideRefIntegrityPreCommitCheck(typeAttrId, toAttrId)(sysPreCommitCheckContext) :: Nil
}

case class TypeIndexAttrCalc(
  typeAttrId: Long, propAttrId: Long,
  relTypeIdToAttrId: Map[String,Long], indexedAttrIds: Set[Long]
)
  (context: SysAttrCalcContext)
  extends AttrCalc
{
  import context._
  def version = UUID.fromString("a6e93a68-1df8-4ee7-8b3f-1cb5ae768c42")
  def affectedByAttrIds = typeAttrId :: propAttrId :: Nil
  def recalculate(objId: Long) = {
    indexSearch(objId)
      .foreach(attrId => if(indexedAttrIds(attrId)) db(objId, attrId) = LMRemoved)
    (db(objId, typeAttrId), db(objId, propAttrId)) match {
      case (_,LMRemoved) | (LMRemoved,_) => ()
      case (LMStringValue(typeIdStr),value) =>
        val attrIdOpt: Option[Long] = relTypeIdToAttrId.get(typeIdStr)
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
  private def dbHas(objId: Long, attrId: Long) = db(objId, attrId) != LMRemoved
  protected def typeAttrId: Long
  protected def toAttrId: Long
  protected def checkAll(objIds: Seq[Long]): Unit
  protected def check(objId: Long) = db(objId, toAttrId) match {
    case LMRemoved ⇒ ()
    case LMLongValue(toObjId) if dbHas(toObjId, typeAttrId) ⇒ ()
    case v => fail(objId, s"attr $toAttrId should refer to valid object, but $v found")
  }
  def recalculate(objId: Long) = add(objId)
  private lazy val add = preCommitCalcCollector(checkAll)
}

//toAttrId must be indexed
case class TypeRefIntegrityPreCommitCheck(typeAttrId: Long, toAttrId: Long)
  (context: SysPreCommitCheckContext)
  extends RefIntegrityPreCommitCheck(context)
{
  import context._
  def version = UUID.fromString("b2232ecf-734c-4cfa-a88f-78b066a01cd3")
  def affectedByAttrIds = typeAttrId :: Nil
  private def referredBy(objId: Long): List[Long] =
    indexSearch(toAttrId, new LMLongValue(objId))
  protected def checkAll(objIds: Seq[Long]) =
    objIds.flatMap(referredBy).foreach(check)
}

case class SideRefIntegrityPreCommitCheck(typeAttrId: Long, toAttrId: Long)
  (context: SysPreCommitCheckContext)
  extends RefIntegrityPreCommitCheck(context)
{
  def version = UUID.fromString("677f2fdc-b56e-4cf8-973f-db148ee3f0c4")
  def affectedByAttrIds = toAttrId :: Nil
  protected def checkAll(objIds: Seq[Long]) = objIds.foreach(check)
}
