package ee.cone.base.db

import ee.cone.base.connection_api.{Attr, CoHandler, BaseCoHandler}

class MandatoryImpl(
  attrFactory: AttrFactory, factIndex: FactIndex,
  preCommitCheck: PreCommitCheckAllOfConnection
  //attrs: ListByDBNode
) extends Mandatory {
  def apply(condAttr: Attr[_], mandatoryAttr: Attr[_], mutual: Boolean): List[BaseCoHandler] =
    apply(condAttr, mandatoryAttr) ::: (if(mutual) apply(mandatoryAttr, condAttr) ::: Nil else Nil)
  def apply(condAttr: Attr[_], mandatoryAttr: Attr[_]): List[BaseCoHandler] =
    handlers(attrFactory.attrId(condAttr), attrFactory.attrId(mandatoryAttr))
  def handlers(condAttrId: ObjId, mandatoryAttrId: ObjId): List[BaseCoHandler] = {
    (condAttrId :: mandatoryAttrId :: Nil).map{ attrId =>
      CoHandler(AfterUpdate(attrId))(preCommitCheck.create(nodes=>
        for(node ← nodes if node(factIndex.defined(condAttrId)) && !node(factIndex.defined(mandatoryAttrId)))
          yield ValidationFailure(s"mandatory $condAttrId => $mandatoryAttrId", node)
      ))
    }
  }
}
