package ee.cone.base.db

class MandatoryPreCommitCheckList(preCommitCheck: PreCommitCheck=>AttrCalc) {
  def apply(condAttr: Prop[_], mandatoryAttr: Prop[_], mutual: Boolean): List[AttrCalc] =
    preCommitCheck(MandatoryPreCommitCheck(condAttr, mandatoryAttr)) :: (
      if(mutual) preCommitCheck(MandatoryPreCommitCheck(mandatoryAttr, condAttr)) :: Nil
      else Nil
    )
}

case class MandatoryPreCommitCheck(condAttr: Prop[_], mandatoryAttr: Prop[_]) extends PreCommitCheck {
  def affectedBy = condAttr :: mandatoryAttr :: Nil
  def check(nodes: Seq[DBNode]) =
    for(node ← nodes if node(condAttr.nonEmpty) && !node(mandatoryAttr.nonEmpty))
      yield ValidationFailure(this, node)
}
