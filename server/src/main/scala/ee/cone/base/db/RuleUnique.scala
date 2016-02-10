
package ee.cone.base.db

class UniqueAttrCalcList(preCommitCheck: PreCommitCheck=>AttrCalc) {
  def apply(searchUniqueAttr: SearchByValue[DBValue]) =
    preCommitCheck(UniqueAttrCalc(searchUniqueAttr)) :: Nil
}

//uniqueAttrId must be indexed
case class UniqueAttrCalc(
  searchUniqueAttr: SearchByValue[DBValue],
  version: String = "2a734606-11c4-4a7e-a5de-5486c6b788d2"
) extends PreCommitCheck {
  private def uniqueAttr = searchUniqueAttr.direct.ruled
  def affectedBy = uniqueAttr :: Nil
  def check(objIds: Seq[ObjId]) = objIds.flatMap{ objId =>
    uniqueAttr(objId) match {
      case DBRemoved => Nil
      case uniqueValue => searchUniqueAttr(uniqueValue) match {
        case id :: Nil => Nil
        case ids => ids.map(ValidationFailure(this,_))
      }
    }
  }
}
