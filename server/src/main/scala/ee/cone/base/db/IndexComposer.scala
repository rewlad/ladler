package ee.cone.base.db

import ee.cone.base.util.Never

object IndexComposerImpl extends IndexComposer {
  def apply(labelAttr: CalcIndex, propAttr: CalcIndex) = {
    val AttrId(labelAttrId,0) = labelAttr.attrId
    val AttrId(0,propAttrId) = propAttr.attrId
    val composedAttrId = new AttrId(labelAttrId,propAttrId)
    new CalcIndex {
      def set(node: DBNode, value: DBValue) = Never()
      def attrId = composedAttrId
      def get(node: DBNode) =
        if(node(labelAttr)==DBRemoved) DBRemoved else node(propAttr)
      def affects(calc: AttrCalc) = {
        labelAttr.affects(calc)
        propAttr.affects(calc)
      }
    }
  }
}

