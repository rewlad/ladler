package ee.cone.base.db

import ee.cone.base.connection_api.{LifeCycle, Registration}
import ee.cone.base.db.Types._

class PreCommitCheckOfTx(val calc: PreCommitCheck, var objIds: List[DBNode] = Nil)

class PreCommitCheckAllOfTx(lifeCycle: LifeCycle, checkAll: PreCommitCheckAllOfConnection) extends Registration with TxComponent {
  private var checkList: List[PreCommitCheckOfTx] = Nil
  def open() = checkAll.txOpt = Some(this)
  def close() = checkAll.txOpt = None
  def add(calc: PreCommitCheckAttrCalcImpl, node: DBNode) = {
    if(calc.txOpt.isEmpty){
      val check = lifeCycle.setup(new PreCommitCheckOfTx(calc.check))(_=>calc.txOpt = None)
      calc.txOpt = Some(check)
      checkList = check :: checkList
    }
    calc.txOpt.get.objIds = node :: calc.txOpt.get.objIds
  }
  def check(): Seq[ValidationFailure] =
    checkList.flatMap(check=>check.calc.doCheckAll(check.objIds.distinct))
}

case class PreCommitCheckAttrCalcImpl(check: PreCommitCheck)(
  checkAll: PreCommitCheckAllOfConnection, createNode: ObjId=>DBNode
) extends AttrCalc {
  var txOpt: Option[PreCommitCheckOfTx] = None
  def affectedBy = check.affectedBy.map(_.nonEmpty)
  def beforeUpdate(objId: ObjId) = ()
  def afterUpdate(objId: ObjId) = checkAll.txOpt.get.add(this, createNode(objId))
  def doCheckAll(objIds: Seq[ObjId]) = check.check(objIds.map())
}

class PreCommitCheckAllOfConnection {
  var txOpt: Option[PreCommitCheckAllOfTx] = None
  def apply() = txOpt.get.check()
}

