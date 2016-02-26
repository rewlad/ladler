package ee.cone.base.db

import ee.cone.base.connection_api.{LifeCycle, Registration}
import ee.cone.base.db.Types._
import ee.cone.base.util.Setup

import scala.collection.mutable

class PreCommitCheckOfTx(check: PreCommitCheck){
  var nodes: List[DBNode] = Nil
  def add(node: DBNode): Unit = nodes = node :: nodes
  def checkAll() = check.check(nodes.distinct)
}

class PreCommitCheckAllOfTx(
  checkAllOfConnection: PreCommitCheckAllOfConnection
) {
  private lazy val checkMap = mutable.Map[AttrCalc,PreCommitCheckOfTx]()
  private var checkList: List[PreCommitCheckOfTx] = Nil
  def of(calc: PreCommitCheckAttrCalcImpl) = checkMap.getOrElseUpdate(calc,
    Setup(new PreCommitCheckOfTx(calc.check()))(c=>checkList = c :: checkList)
  )
  def checkAll(): Seq[ValidationFailure] = checkList.flatMap(check=>check.checkAll())
}

class PreCommitCheckAttrCalcImpl(
  val check: ()=>PreCommitCheck,
  checkAll: PreCommitCheckAllOfConnectionImpl
) extends AttrCalc {
  def affectedBy = check().affectedBy.map(_.nonEmpty)
  def beforeUpdate(node: DBNode) = ()
  def afterUpdate(node: DBNode) = checkAll.currentTx.of(this).add(node)
}

class PreCommitCheckAllOfConnectionImpl extends PreCommitCheckAllOfConnection {
  private var txOpt: Option[PreCommitCheckAllOfTx] = None
  def currentTx = txOpt.get
  def switchTx(on: Option[Unit]) =
    txOpt = on.map(_=>new PreCommitCheckAllOfTx(this))
  def checkTx() = currentTx.checkAll()
}
