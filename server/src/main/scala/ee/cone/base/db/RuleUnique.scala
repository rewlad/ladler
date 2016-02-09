
package ee.cone.base.db

import java.util.UUID

class UniqueAttrCalcList(preCommitCalcCollector: PreCommitCalcCollector) {
  def apply(uniqueAttr: RuledIndex, searchUniqueAttr: AttrIndex[DBValue,List[ObjId]]) =
    UniqueAttrCalc(uniqueAttr, searchUniqueAttr)(preCommitCalcCollector) :: Nil
}

//uniqueAttrId must be indexed
case class UniqueAttrCalc(
  uniqueAttr: RuledIndex,
  searchUniqueAttr: AttrIndex[DBValue,List[ObjId]],
  version: String = "2a734606-11c4-4a7e-a5de-5486c6b788d2"
)(
  preCommitCalcCollector: PreCommitCalcCollector
) extends AttrCalc {
  def affectedBy = uniqueAttr :: Nil
  def recalculate(objId: ObjId) = add(objId)
  private lazy val add = preCommitCalcCollector{ objIds =>
    objIds.flatMap{ objId =>
      uniqueAttr(objId) match {
        case DBRemoved => Nil
        case uniqueValue => searchUniqueAttr(uniqueValue) match {
          case id :: Nil => Nil
          case ids => ids.map(ValidationFailure(this,_))
        }
      }
    }
  }
}
