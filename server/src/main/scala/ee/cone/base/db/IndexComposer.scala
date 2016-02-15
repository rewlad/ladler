package ee.cone.base.db

import ee.cone.base.util.Never

object IndexComposerImpl extends IndexComposer {
  def apply(labelAttr: CalcIndex, propAttr: CalcIndex) = {
    val AttrId(labelAttrId,0) = labelAttr.attrId
    val AttrId(0,propAttrId) = propAttr.attrId
    val composedAttrId = new AttrId(labelAttrId,propAttrId)
    new CalcIndex {
      def update(objId: ObjId, value: DBValue) = Never()
      def attrId = composedAttrId
      def apply(objId: ObjId) =
        if(labelAttr(objId)==DBRemoved) DBRemoved else propAttr(objId)
      protected def affects(calc: AttrCalc) = {
        labelAttr.affects(calc)
        propAttr.affects(calc)
      }
    }
  }
}

