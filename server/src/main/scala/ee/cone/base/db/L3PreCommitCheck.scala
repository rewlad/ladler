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
) extends Registration with TxComponent {
  private lazy val checkMap = mutable.Map[AttrCalc,PreCommitCheckOfTx]()
  private var checkList: List[PreCommitCheckOfTx] = Nil
  def open() = checkAllOfConnection.txOpt = Some(this)
  def close() = checkAllOfConnection.txOpt = None
  def of(calc: PreCommitCheckAttrCalcImpl) = checkMap.getOrElseUpdate(calc,
    Setup(new PreCommitCheckOfTx(calc.check()))(c=>checkList = c :: checkList)
  )
  def checkAll(): Seq[ValidationFailure] = checkList.flatMap(check=>check.checkAll())
}

class PreCommitCheckAttrCalcImpl(
  val check: ()=>PreCommitCheck,
  checkAll: PreCommitCheckAllOfConnection
) extends AttrCalc {
  var txOpt: Option[PreCommitCheckOfTx] = None
  def affectedBy = check().affectedBy.map(_.nonEmpty)
  def beforeUpdate(node: DBNode) = ()
  def afterUpdate(node: DBNode) = checkAll.txOpt.get.of(this).add(node)
}

class PreCommitCheckAllOfConnection {
  var txOpt: Option[PreCommitCheckAllOfTx] = None
  def apply() = txOpt.get.checkAll()
}
