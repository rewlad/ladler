package ee.cone.base.db

import java.util.UUID

case class DeleteAttrCalc(typeAttrId: Long)(context: SysAttrCalcContext) extends AttrCalc {
  import context._
  private def dbHas(objId: Long, attrId: Long) = db(objId, attrId) != LMRemoved
  def version = UUID.fromString("a9e66744-883f-47c9-9cda-ed5b9c1a11bb")
  def affectedByAttrIds = typeAttrId :: Nil
  def recalculate(objId: Long) = {
    if(!dbHas(objId, typeAttrId))
      indexSearch(objId)
        .foreach(attrId => db.set(objId, attrId, LMRemoved, db.isOriginal)) // can override original
  }
}
