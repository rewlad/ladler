package ee.cone.base.db_impl

import ee.cone.base.connection_api.{Attr, BaseCoHandler, CoHandler, Obj}
import ee.cone.base.db.{InheritAttrRule, MainEnvKey, CurrentTx,
SearchByLabelProp}

class InheritAttrRuleImpl(
  attrFactory: AttrFactoryI,
  findNodes: FindNodesI,
  mainTx: CurrentTx[MainEnvKey]
) extends InheritAttrRule {
  def apply[Value](fromAttr: Attr[Value], toAttr: Attr[Value], byIndex: SearchByLabelProp[Obj]): List[BaseCoHandler] = {
    def copy(fromObj: Obj, toObj: Obj): Unit = toObj(toAttr) = fromObj(fromAttr)
    CoHandler(AfterUpdate(attrFactory.attrId(fromAttr)))(fromObj ⇒
      findNodes.where(mainTx(), byIndex, fromObj, Nil).foreach(toObj⇒
        copy(fromObj,toObj)
      )
    ) ::
      CoHandler(AfterUpdate(byIndex.propId)){ toObj ⇒
        val byAttr = attrFactory.toAttr(byIndex.propId, byIndex.propType)
        val fromObj = toObj(byAttr)
        copy(fromObj, toObj)
      } :: Nil
  }
}
