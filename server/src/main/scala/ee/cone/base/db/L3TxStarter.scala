package ee.cone.base.db

import ee.cone.base.connection_api.LifeCycle
import ee.cone.base.util.Never

class TxManagerImpl(
  connectionLifeCycle: LifeCycle, env: DBEnv,
  checkAll: PreCommitCheckAllOfConnection
) extends TxManager {
  var txOpt: Option[RawTx] = None
  def tx = txOpt.get
  def needTx(rw: Boolean): Unit = {
    if(txOpt.isEmpty) {
      val lifeCycle = connectionLifeCycle.sub()
      val rawTx = lifeCycle.of(()=>env.createTx(lifeCycle, rw = rw)).updates(txOpt=_).value
      lifeCycle.of(()=>()).updates(checkAll.switchTx(rawTx,_))
    }
    if(tx.rw != rw) Never()
  }
  def commit() = {
    if(!tx.rw) Never()
    val fails = checkAll.checkTx(tx)
    if(fails.nonEmpty) throw new Exception(s"$fails")
    closeTx()
  }
  def closeTx() = txOpt.foreach(_.lifeCycle.close())
}
