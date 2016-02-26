package ee.cone.base.db

import ee.cone.base.db.Types._

class DeleteAttrCalcList(
  typeId: Attr[_],
  attrs: ListByDBNode
) {
  def apply() = DeleteAttrCalc(typeId, attrs) :: Nil
}

case class DeleteAttrCalc[T,A](typeId: Attr[_], attrs: ListByDBNode) extends AttrCalc {
  def affectedBy = typeId.nonEmpty :: Nil
  def afterUpdate(node: DBNode) = if(!node(typeId.nonEmpty))
    attrs.list(node).foreach(attr => node(attr.nonEmpty) = false)
  def beforeUpdate(node: DBNode) = ()
}
