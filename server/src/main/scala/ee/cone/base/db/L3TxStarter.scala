package ee.cone.base.db

import ee.cone.base.connection_api.LifeCycle
import ee.cone.base.util.Never

class CurrentTxImpl[DBEnvKey] extends CurrentTx[DBEnvKey] {
  def apply() = value.get
  var value: Option[RawTx] = None
}

class TxManagerImpl[DBEnvKey](
  connectionLifeCycle: LifeCycle, env: DBEnv, currentTx: CurrentTxImpl[DBEnvKey],
  checkAll: PreCommitCheckAllOfConnection
) extends TxManager[DBEnvKey] {

  def tx = currentTx()
  def needTx(rw: Boolean): Unit = {
    if(currentTx.value.isEmpty) {
      val lifeCycle = connectionLifeCycle.sub()
      val rawTx = lifeCycle.of(()=>env.createTx(lifeCycle, rw = rw)).updates(currentTx.value=_).value
      lifeCycle.of(()=>()).updates(checkAll.switchTx(rawTx,_))
    }
    if(currentTx().rw != rw) Never()
  }
  def commit() = {
    if(!currentTx().rw) Never()
    val fails = checkAll.checkTx(currentTx())
    if(fails.nonEmpty) throw new Exception(s"$fails")
    closeTx()
  }
  def closeTx() = currentTx.value.foreach(_.lifeCycle.close())
}
