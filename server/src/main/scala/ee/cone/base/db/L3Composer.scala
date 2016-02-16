package ee.cone.base.db

import ee.cone.base.db.Types._
import ee.cone.base.util.Never

object IndexComposerImpl extends IndexComposer {
  def apply(labelAttr: CalcFactIndex, propAttr: CalcFactIndex) = {
    val AttrId(labelAttrId,0) = labelAttr.attrId
    val AttrId(0,propAttrId) = propAttr.attrId
    val composedAttrId = new AttrId(labelAttrId,propAttrId)
    new CalcFactIndex {
      def set(objId: ObjId, value: DBValue) = Never()
      def attrId = composedAttrId
      def get(objId: ObjId) =
        if(labelAttr.get(objId)==DBRemoved) DBRemoved else propAttr.get(objId)
      def affects = labelAttr.affects ::: propAttr.affects
    }
  }
}

