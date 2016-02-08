package ee.cone.base.db

import java.util.UUID

class MandatoryPreCommitCheckList(context: SysPreCommitCheckContext) {
  def apply(condAttrId: AttrId, mandatoryAttrId: AttrId, mutual: Boolean): List[AttrCalc] =
    MandatoryPreCommitCheck(condAttrId, mandatoryAttrId)(context) ::
      (if(mutual) MandatoryPreCommitCheck(mandatoryAttrId, condAttrId)(context) :: Nil else Nil)
}

case class MandatoryPreCommitCheck(condAttrId: AttrId, mandatoryAttrId: AttrId)
  (context: SysPreCommitCheckContext)
  extends AttrCalc
{
  import context._
  private def dbHas(objId: ObjId, attrId: AttrId) = db(objId, attrId) != DBRemoved
  def version = UUID.fromString("ed748474-04e0-4ff7-89a1-be8a95aa743c")
  def affectedByAttrIds = condAttrId :: mandatoryAttrId :: Nil
  def recalculate(objId: ObjId) = add(objId)
  private lazy val add = preCommitCalcCollector{ objIds =>
    for(objId ← objIds)
      if(dbHas(objId, condAttrId) && !dbHas(objId, mandatoryAttrId))
        fail(objId, s"has $condAttrId, but not $mandatoryAttrId")
  }
}
