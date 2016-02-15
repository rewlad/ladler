package ee.cone.base.db

class DeleteAttrCalcList(
  typeId: CalcIndex,
  attrs: SearchByNode
) {
  def apply() = DeleteAttrCalc(typeId, attrs) :: Nil
}

case class DeleteAttrCalc(
  typeId: CalcIndex,
  attrs: SearchByNode,
  version: String = "a9e66744-883f-47c9-9cda-ed5b9c1a11bb"
) extends AttrCalc {
  def affectedBy = typeId :: Nil
  def beforeUpdate(node: DBNode) = ()
  def afterUpdate(node: DBNode) =
    if(node(typeId)==DBRemoved) node(attrs).foreach(node(_) = DBRemoved)
}
