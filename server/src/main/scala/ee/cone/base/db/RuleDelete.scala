package ee.cone.base.db

class DeleteAttrCalcList(
  typeId: RuledIndex,
  attrs: AttrIndex[ObjId,List[RuledIndex]]
) {
  def apply(typeAttrId: AttrId) = DeleteAttrCalc(typeId, attrs) :: Nil
}

case class DeleteAttrCalc(
  typeId: RuledIndex,
  attrs: AttrIndex[ObjId,List[RuledIndex]],
  version: String = "a9e66744-883f-47c9-9cda-ed5b9c1a11bb"
) extends AttrCalc {
  def affectedBy = typeId :: Nil
  def recalculate(objId: ObjId) =
    if(typeId(objId)==DBRemoved) attrs(objId).foreach(_(objId) = DBRemoved)
}
