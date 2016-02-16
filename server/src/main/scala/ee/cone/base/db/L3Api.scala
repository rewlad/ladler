package ee.cone.base.db

import ee.cone.base.db.Types._

trait AttrInfo

trait Affecting {
  def affects: List[CalcFactIndex]
}

trait Affected {
  def affectedBy: List[CalcFactIndex]
}

trait AttrCalc extends AttrInfo with Affected {
  def beforeUpdate(objId: ObjId): Unit
  def afterUpdate(objId: ObjId): Unit
}

trait CalcFactIndex extends AttrInfo with Affecting {
  def attrId: AttrId
  def get(objId: ObjId): DBValue
  def set(objId: ObjId, value: DBValue): Unit
}

trait CalcSearchIndex {
  def execute(value: DBValue, feed: Feed[ObjId]): Unit
  def execute(value: DBValue, objId: ObjId, feed: Feed[ObjId]): Unit
}

trait IndexComposer {
  def apply(labelAttr: CalcFactIndex, propAttr: CalcFactIndex): CalcFactIndex
}

case class ValidationFailure(calc: PreCommitCheck, objId: ObjId)

trait PreCommitCheckAttrCalc extends AttrCalc {
  def checkAll(): Seq[ValidationFailure]
}

trait PreCommitCheck extends Affected {
  def check(ibjIds: Seq[ObjId]): Seq[ValidationFailure]
}

trait CalcFactIndexByObjId {
  def execute(objId: ObjId, feed: Feed[CalcFactIndex]): Unit
}
