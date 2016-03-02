package ee.cone.base.db

import ee.cone.base.connection_api.CoHandler
import ee.cone.base.db.Types._

class DeleteAttrCalcList(
  typeId: Attr[_],
  attrs: ListByDBNode
) {
  def apply() = CoHandler[DBNode,Unit](AfterUpdate(typeId.nonEmpty) :: Nil){ node =>
    if(!node(typeId.nonEmpty))
      attrs.list(node).foreach(attr => node(attr.nonEmpty) = false)
  } :: Nil
}
