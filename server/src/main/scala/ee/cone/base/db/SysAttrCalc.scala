package ee.cone.base.db

import java.util.UUID

import ee.cone.base.util.Never

class SysAttrCalcFactory(
  val db: IndexingTx, val indexSearch: IndexSearch, val fail: ValidateFailReaction // fail'll probably do nothing in case of outdated rel type
) extends DBHas {
  def createTypeIndexAttrCalc(
    typeAttrId: Long, propAttrId: Long,
    relTypeIdToAttrId: Map[String,Long], indexedAttrIds: Set[Long]
  ) = TypeIndexAttrCalc(typeAttrId, propAttrId, relTypeIdToAttrId, indexedAttrIds)(this)
  def createLabelIndexAttrCalc(labelAttrId: Long, propAttrId: Long, indexedAttrId: Long) =
    LabelIndexAttrCalc(labelAttrId, propAttrId, indexedAttrId)(this)
  def createDeleteAttrCalc(typeAttrId: Long) = DeleteAttrCalc(typeAttrId)(this)
}

case class TypeIndexAttrCalc(
  typeAttrId: Long, propAttrId: Long,
  relTypeIdToAttrId: Map[String,Long], indexedAttrIds: Set[Long]
)
  (context: SysAttrCalcFactory)
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

case class LabelIndexAttrCalc(labelAttrId: Long, propAttrId: Long, indexedAttrId: Long)
  (context: SysAttrCalcFactory)
  extends AttrCalc
{
  import context._
  def version = UUID.fromString("1afd3999-46ac-4da3-84a6-17d978f7e032")
  def affectedByAttrIds = labelAttrId :: propAttrId :: Nil
  def recalculate(objId: Long) = db(objId, indexedAttrId) =
    if(!dbHas(objId, labelAttrId)) LMRemoved else db(objId, propAttrId)
}

case class DeleteAttrCalc(typeAttrId: Long)(context: SysAttrCalcFactory) extends AttrCalc {
  import context._
  def version = UUID.fromString("a9e66744-883f-47c9-9cda-ed5b9c1a11bb")
  def affectedByAttrIds = typeAttrId :: Nil
  def recalculate(objId: Long) = {
    if(!dbHas(objId, typeAttrId))
      indexSearch(objId)
        .foreach(attrId => db.set(objId, attrId, LMRemoved, db.isOriginal)) // can override original
  }
}

////////////////////////////////////////////////////////////////////////////////

class SysPreCommitCheckFactory(
  val db: IndexingTx, val indexSearch: IndexSearch,
  val preCommitCalcCollector: PreCommitCalcCollector,
  val fail: ValidateFailReaction
) extends PreCommitCalcContext with DBHas {
  def createMutualMandatoryPreCommitCheckList(aAttrId: Long, bAttrId: Long): List[AttrCalc] =
    MandatoryPreCommitCheck(aAttrId, bAttrId)(this) ::
    MandatoryPreCommitCheck(bAttrId, aAttrId)(this) :: Nil
  def createRefIntegrityPreCommitCheckList(typeAttrId: Long, toAttrId: Long): List[AttrCalc] =
    TypeRefIntegrityPreCommitCheck(typeAttrId, toAttrId)(this) ::
    SideRefIntegrityPreCommitCheck(typeAttrId, toAttrId)(this) :: Nil
}

case class MandatoryPreCommitCheck(condAttrId: Long, mandatoryAttrId: Long)
  (context: SysPreCommitCheckFactory)
  extends PreCommitCalc(context)
{
  import context._
  def version = UUID.fromString("ed748474-04e0-4ff7-89a1-be8a95aa743c")
  def affectedByAttrIds = condAttrId :: mandatoryAttrId :: Nil
  def apply() = for(objId ← objIds)
    if(dbHas(objId, condAttrId) && !dbHas(objId, mandatoryAttrId))
      fail(objId, s"has $condAttrId, but not $mandatoryAttrId")
}

abstract class RefIntegrityPreCommitCheck(context: SysPreCommitCheckFactory)
  extends PreCommitCalc(context)
{
  import context._
  protected def typeAttrId: Long
  protected def toAttrId: Long
  protected def check(objId: Long) = db(objId, toAttrId) match {
    case LMRemoved ⇒ ()
    case LMLongValue(toObjId) if dbHas(toObjId, typeAttrId) ⇒ ()
    case v => fail(objId, s"attr $toAttrId should refer to valid object, but $v found")
  }
}

//toAttrId must be indexed
case class TypeRefIntegrityPreCommitCheck(typeAttrId: Long, toAttrId: Long)
  (context: SysPreCommitCheckFactory)
  extends RefIntegrityPreCommitCheck(context)
{
  import context._
  def version = UUID.fromString("b2232ecf-734c-4cfa-a88f-78b066a01cd3")
  def affectedByAttrIds = typeAttrId :: Nil
  private def referredBy(objId: Long): List[Long] =
    indexSearch(toAttrId, new LMLongValue(objId))
  def apply() = objIds.flatMap(referredBy).foreach(check)
}

case class SideRefIntegrityPreCommitCheck(typeAttrId: Long, toAttrId: Long)
  (context: SysPreCommitCheckFactory)
  extends RefIntegrityPreCommitCheck(context)
{
  def version = UUID.fromString("677f2fdc-b56e-4cf8-973f-db148ee3f0c4")
  def affectedByAttrIds = toAttrId :: Nil
  def apply() = objIds.foreach(check)
}
