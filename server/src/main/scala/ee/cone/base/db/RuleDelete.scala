package ee.cone.base.db

import ee.cone.base.connection_api.CoHandler
import ee.cone.base.db.Types._

class DeleteAttrCalcList(
  typeId: Attr[_],
  attrs: ListByDBNode
) {
  def apply() = new DeleteAttrCalc(typeId, attrs) :: Nil
}

class DeleteAttrCalc[T,A](typeId: Attr[_], attrs: ListByDBNode) extends CoHandler[DBNode,Unit] {
  def on = AfterUpdate(typeId.nonEmpty) :: Nil
  def handle(node: DBNode) = if(!node(typeId.nonEmpty))
    attrs.list(node).foreach(attr => node(attr.nonEmpty) = false)
}
