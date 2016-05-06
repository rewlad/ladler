package ee.cone.base.db

import ee.cone.base.connection_api.{Obj, Attr, CoHandler}
import ee.cone.base.db.Types._

class DeleteAttrCalcList(
  typeIdDefined: Attr[Boolean],
  attrs: ListByDBNode
) {
  def apply() = CoHandler[Obj=>Unit](AfterUpdate(typeIdDefined)){ node =>
    if(!node(typeIdDefined)) node(attrs) = Nil
  } :: Nil
}
