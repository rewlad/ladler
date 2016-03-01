package ee.cone.base.db

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
  private lazy val checkMap = mutable.Map[CoHandler[DBNode,Unit],PreCommitCheckOfTx]()
  private var checkList: List[PreCommitCheckOfTx] = Nil
  def of(calc: PreCommitCheckAttrCalcImpl) = checkMap.getOrElseUpdate(calc,
    Setup(new PreCommitCheckOfTx(calc.check()))(c=>checkList = c :: checkList)
  )
  def checkAll(): Seq[ValidationFailure] = checkList.flatMap(check=>check.checkAll())
}

class PreCommitCheckAttrCalcImpl(
  val check: ()=>PreCommitCheck,
  checkAll: PreCommitCheckAllOfConnectionImpl
) extends CoHandler[DBNode,Unit] {
  def on = check().affectedBy.map(_.nonEmpty).map(AfterUpdate)
  def handle(node: DBNode) = checkAll.txOf(node).of(this).add(node)
}

class PreCommitCheckAllOfConnectionImpl extends PreCommitCheckAllOfConnection {
  private var txs = mutable.Map[RawTx,PreCommitCheckAllOfTx]()
  def txOf(node: DBNode) = txs(node.tx)
  def switchTx(tx: RawTx, on: Option[Unit]) =
    if(on.nonEmpty) txs(tx) = new PreCommitCheckAllOfTx(this) else txs.remove(tx)
  def checkTx(tx: RawTx) = txs(tx).checkAll()
}
