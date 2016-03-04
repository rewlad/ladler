package ee.cone.base.db

import ee.cone.base.connection_api.{CoHandler, BaseCoHandler}
import ee.cone.base.util.Setup

import scala.collection.mutable

class PreCommitCheckOfTx(check: Seq[DBNode]=>Seq[ValidationFailure]){
  var nodes: List[DBNode] = Nil
  def add(node: DBNode): Unit = nodes = node :: nodes
  def checkAll() = check(nodes.distinct)
}

class PreCommitCheckAllOfTx(
  checkAllOfConnection: PreCommitCheckAllOfConnection
) {
  private lazy val checkMap = mutable.Map[Seq[DBNode]=>Seq[ValidationFailure],PreCommitCheckOfTx]()
  private var checkList: List[PreCommitCheckOfTx] = Nil
  def of(calc: Seq[DBNode]=>Seq[ValidationFailure]) = checkMap.getOrElseUpdate(calc,
    Setup(new PreCommitCheckOfTx(calc))(c=>checkList = c :: checkList)
  )
  def checkAll(): Seq[ValidationFailure] = checkList.flatMap(check=>check.checkAll())
}

class PreCommitCheckAllOfConnectionImpl extends PreCommitCheckAllOfConnection {
  private var txs = mutable.Map[RawTx,PreCommitCheckAllOfTx]()
  def switchTx(tx: RawTx, on: Boolean) =
    if(on) txs(tx) = new PreCommitCheckAllOfTx(this) else txs.remove(tx)
  def checkTx(tx: RawTx) = txs(tx).checkAll()
  def create(later: Seq[DBNode]=>Seq[ValidationFailure]): DBNode=>Unit =
    node => txs(node.tx).of(later).add(node)
}
