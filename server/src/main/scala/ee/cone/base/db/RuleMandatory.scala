package ee.cone.base.db

import ee.cone.base.connection_api.{Attr, CoHandler, BaseCoHandler}

class MandatoryImpl(preCommitCheck: PreCommitCheckAllOfConnection, attrs: ListByDBNode) extends Mandatory {
  def apply(condAttr: Attr[_], mandatoryAttr: Attr[_], mutual: Boolean): List[BaseCoHandler] =
    apply(condAttr, mandatoryAttr) ::: (if(mutual) apply(mandatoryAttr, condAttr) ::: Nil else Nil)
  def apply(condAttr: Attr[_], mandatoryAttr: Attr[_]): List[BaseCoHandler] = {
    (condAttr :: mandatoryAttr :: Nil).map{ a =>
      CoHandler(AfterUpdate(a.defined))(preCommitCheck.create(nodes=>
        for(node ← nodes if node(condAttr.defined) && !node(mandatoryAttr.defined))
          yield ValidationFailure(s"mandatory $condAttr => $mandatoryAttr -- ${node(attrs)}", node)
      ))
    }
  }
}
