package ee.cone.base.db

import ee.cone.base.db.Types._

class MandatoryPreCommitCheckList(preCommitCheck: PreCommitCheck=>NodeHandler[Unit]) {
  def apply(condAttr: Attr[_], mandatoryAttr: Attr[_], mutual: Boolean): List[NodeHandler[_]] =
    preCommitCheck(MandatoryPreCommitCheck(condAttr, mandatoryAttr)) :: (
      if(mutual) preCommitCheck(MandatoryPreCommitCheck(mandatoryAttr, condAttr)) :: Nil
      else Nil
    )
}

case class MandatoryPreCommitCheck(condAttr: Attr[_], mandatoryAttr: Attr[_]) extends PreCommitCheck {
  def affectedBy = condAttr :: mandatoryAttr :: Nil
  def check(nodes: Seq[DBNode]) =
    for(node ← nodes if node(condAttr.nonEmpty) && !node(mandatoryAttr.nonEmpty))
      yield ValidationFailure(this, node)
}
