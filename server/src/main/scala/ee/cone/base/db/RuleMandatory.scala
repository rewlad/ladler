package ee.cone.base.db

import java.util.UUID

class MandatoryPreCommitCheckList(preCommitCalcCollector: PreCommitCalcCollector) {
  def apply(condAttr: RuledIndex, mandatoryAttr: RuledIndex, mutual: Boolean): List[AttrCalc] =
    MandatoryPreCommitCheck(condAttr, mandatoryAttr)(preCommitCalcCollector) :: (
      if(mutual) MandatoryPreCommitCheck(mandatoryAttr, condAttr)(preCommitCalcCollector) :: Nil
      else Nil
    )
}

case class MandatoryPreCommitCheck(
  condAttr: RuledIndex, mandatoryAttr: RuledIndex,
  version: String = "ed748474-04e0-4ff7-89a1-be8a95aa743c"
)(
  preCommitCalcCollector: PreCommitCalcCollector
) extends AttrCalc {
  def affectedBy = condAttr :: mandatoryAttr :: Nil
  def recalculate(objId: ObjId) = add(objId)
  private lazy val add = preCommitCalcCollector{ objIds =>
    for(objId ← objIds if condAttr(objId)!=DBRemoved && mandatoryAttr(objId)==DBRemoved)
      yield ValidationFailure(this, objId)
  }
}
