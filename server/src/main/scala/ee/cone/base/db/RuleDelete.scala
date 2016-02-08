package ee.cone.base.db

import java.util.UUID

class DeleteAttrCalcList(context: SysAttrCalcContext) {
  def apply(typeAttrId: AttrId) = DeleteAttrCalc(typeAttrId)(context) :: Nil
}

case class DeleteAttrCalc(typeAttrId: AttrId)(context: SysAttrCalcContext) extends AttrCalc {
  import context._
  private def dbHas(objId: ObjId, attrId: AttrId) = db(objId, attrId) != DBRemoved
  def version = UUID.fromString("a9e66744-883f-47c9-9cda-ed5b9c1a11bb")
  def affectedByAttrIds = typeAttrId :: Nil
  def recalculate(objId: ObjId) = if(!dbHas(objId, typeAttrId))
      listAttrIdsByObjId(objId).foreach(attrId => db(objId, attrId) = DBRemoved)
}
