package ee.cone.base.db

import ee.cone.base.connection_api.{CoHandler, BaseCoHandler}

class MandatoryImpl(preCommitCheck: PreCommitCheckAllOfConnection) extends Mandatory {
  def apply(condAttr: Attr[_], mandatoryAttr: Attr[_], mutual: Boolean): List[BaseCoHandler] =
    apply(condAttr, mandatoryAttr) ::: (if(mutual) apply(mandatoryAttr, condAttr) ::: Nil else Nil)
  def apply(condAttr: Attr[_], mandatoryAttr: Attr[_]): List[BaseCoHandler] = {
    val affectedBy = condAttr.defined :: mandatoryAttr.defined :: Nil
    CoHandler(affectedBy.map(AfterUpdate))(preCommitCheck.create(nodes=>
      for(node ← nodes if node(condAttr.defined) && !node(mandatoryAttr.defined))
      yield ValidationFailure("mandatory", node)
    )) :: Nil
  }
}
