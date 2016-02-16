package ee.cone.base.db

class DeleteAttrCalcList(
  typeId: Prop[_],
  attrs: Prop[List[Prop[_]]]
) {
  def apply() = DeleteAttrCalc(typeId, attrs) :: Nil
}

case class DeleteAttrCalc[T,A](typeId: Prop[_], attrs: Prop[List[Prop[_]]]) extends NodeAttrCalc {
  def affectedBy = typeId :: Nil
  def afterUpdate(node: DBNode) = if(!node(typeId.nonEmpty))
    node(attrs).foreach(attr => node(attr.nonEmpty) = false)
  def beforeUpdate(node: DBNode) = ()
}
