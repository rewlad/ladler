package ee.cone.base.db

import ee.cone.base.connection_api.{Obj, Attr, CoHandler}
import ee.cone.base.db.Types._

class DeleteAttrCalcList(
  typeId: Attr[_],
  attrs: ListByDBNode
) {
  def apply() = CoHandler[Obj=>Unit](AfterUpdate(typeId.defined)){ node =>
    if(!node(typeId.defined)) node(attrs) = Nil
  } :: Nil
}
