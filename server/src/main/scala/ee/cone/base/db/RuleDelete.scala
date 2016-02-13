package ee.cone.base.db

class DeleteAttrCalcList(
  typeId: RuledIndex,
  attrs: SearchByObjId
) {
  def apply() = DeleteAttrCalc(typeId, attrs) :: Nil
}

case class DeleteAttrCalc(
  typeId: RuledIndex,
  attrs: SearchByObjId,
  version: String = "a9e66744-883f-47c9-9cda-ed5b9c1a11bb"
) extends AttrCalc {
  def affectedBy = typeId :: Nil
  def beforeUpdate(objId: ObjId) = ()
  def afterUpdate(objId: ObjId) =
    if(typeId(objId)==DBRemoved) attrs(objId).foreach(_(objId) = DBRemoved)
}
