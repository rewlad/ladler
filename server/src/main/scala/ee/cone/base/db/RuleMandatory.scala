package ee.cone.base.db

class MandatoryPreCommitCheckList(preCommitCheck: PreCommitCheck=>AttrCalc) {
  def apply(condAttr: CalcIndex, mandatoryAttr: CalcIndex, mutual: Boolean): List[AttrCalc] =
    preCommitCheck(MandatoryPreCommitCheck(condAttr, mandatoryAttr)) :: (
      if(mutual) preCommitCheck(MandatoryPreCommitCheck(mandatoryAttr, condAttr)) :: Nil
      else Nil
    )
}

case class MandatoryPreCommitCheck(
  condAttr: CalcIndex, mandatoryAttr: CalcIndex,
  version: String = "ed748474-04e0-4ff7-89a1-be8a95aa743c"
) extends PreCommitCheck {
  def affectedBy = condAttr :: mandatoryAttr :: Nil
  def check(objIds: Seq[ObjId]) =
    for(objId ← objIds if condAttr(objId)!=DBRemoved && mandatoryAttr(objId)==DBRemoved)
      yield ValidationFailure(this, objId)
}
