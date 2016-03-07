package ee.cone.base.db

import ee.cone.base.connection_api.LifeCycle
import ee.cone.base.util.{Setup, Never}

class CurrentTxImpl[DBEnvKey] extends CurrentTx[DBEnvKey] {
  var value: Option[BoundToTx] = None
  def apply() = value.get
}

abstract class BaseTxManager[DBEnvKey] {
  protected def currentTx: CurrentTxImpl[DBEnvKey]
  protected def checkAll: PreCommitCheckAllOfConnection

  private var busy = false
  protected def withBusy[R](f: () ⇒ R) = {
    if (busy) Never()
    busy = true
    Setup(f())(_ ⇒ busy = false)
  }
  protected def register(tx: BoundToTx, on: Boolean) = {
    currentTx.value = if(on) Some(tx) else None
    checkAll.switchTx(tx,on)
  }
}

//*Instant, MergerMain
class DefaultTxManagerImpl[DBEnvKey](
    connectionLifeCycle: LifeCycle, env: DBEnv,
    val currentTx: CurrentTxImpl[DBEnvKey],
    val checkAll: PreCommitCheckAllOfConnection
) extends BaseTxManager[DBEnvKey] with DefaultTxManager[DBEnvKey] {
  def rwTx[R](f: () ⇒ R) = withBusy { () ⇒
    env.rwTx { rawIndex ⇒
      val tx = new ProtectedBoundToTx(rawIndex, true)
      register(tx,on=true)
      val res = f()
      val fails = checkAll.checkTx(currentTx())
      if(fails.nonEmpty) throw new Exception(s"$fails")
      register(tx,on=false)
      res
    }
  }
  def roTx[R](f: () ⇒ R) = withBusy { () ⇒
    val lifeCycle = connectionLifeCycle.sub()
    val rawIndex = env.roTx(lifeCycle)
    val tx = new ProtectedBoundToTx(rawIndex, true)
    register(tx,on=true)
    val res = f()
    register(tx,on=false)
    lifeCycle.close()
    res
  }
}

class SessionMainTxManagerImpl(
    connectionLifeCycle: LifeCycle, mainEnv: DBEnv,
    val currentTx: CurrentTxImpl[MainEnvKey],
    val checkAll: PreCommitCheckAllOfConnection,
    muxFactory: MuxFactory
) extends BaseTxManager[MainEnvKey] with SessionMainTxManager {
  class Mux(val tx: ProtectedBoundToTx, val lifeCycle: LifeCycle)
  private var mux: Option[Mux] = None
  def muxTx[R](recreate: Boolean)(f: ()⇒R) = withBusy{ () ⇒
    if(recreate)mux.foreach{ m ⇒
      register(m.tx,on=false)
      mux = None
      m.lifeCycle.close()
    }
    if(mux.isEmpty){
      val lifeCycle = connectionLifeCycle.sub()
      val rawIndex = muxFactory.wrap(mainEnv.roTx(lifeCycle))
      val tx = new ProtectedBoundToTx(rawIndex, false)
      mux = Some(new Mux(tx, lifeCycle))
      register(tx,on=true)
    }
    mux.get.tx.enabled = true
    val res = f()
    mux.get.tx.enabled = false
    res
  }
}

