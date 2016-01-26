package ee.cone.base.db

import java.util.UUID

class MutualMandatoryPreCommitCheckList(context: SysPreCommitCheckContext) {
  def apply(aAttrId: Long, bAttrId: Long): List[AttrCalc] =
    MandatoryPreCommitCheck(aAttrId, bAttrId)(context) ::
    MandatoryPreCommitCheck(bAttrId, aAttrId)(context) :: Nil
}

case class MandatoryPreCommitCheck(condAttrId: Long, mandatoryAttrId: Long)
  (context: SysPreCommitCheckContext)
  extends AttrCalc
{
  import context._
  private def dbHas(objId: Long, attrId: Long) = db(objId, attrId) != LMRemoved
  def version = UUID.fromString("ed748474-04e0-4ff7-89a1-be8a95aa743c")
  def affectedByAttrIds = condAttrId :: mandatoryAttrId :: Nil
  def recalculate(objId: Long) = add(objId)
  private lazy val add = preCommitCalcCollector{ objIds =>
    for(objId ← objIds)
      if(dbHas(objId, condAttrId) && !dbHas(objId, mandatoryAttrId))
        fail(objId, s"has $condAttrId, but not $mandatoryAttrId")
  }
}
