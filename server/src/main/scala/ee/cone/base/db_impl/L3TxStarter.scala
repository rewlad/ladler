package ee.cone.base.db_impl

import ee.cone.base.connection_api._
import ee.cone.base.db._
import ee.cone.base.util.{Setup, Never}

class ProtectedBoundToTx[DBEnvKey](val rawIndex: RawIndex, var enabled: Boolean) extends BoundToTx // not case

class CurrentTxImpl[DBEnvKey](env: DBEnv[DBEnvKey]) extends CurrentTx[DBEnvKey] {
  def dbId = env.dbId
  var value: Option[BoundToTx] = None
  def apply() = {
    if(value.isEmpty) throw new Exception(s"db #$dbId is out of tx")
    value.get
  }
}

class TxSelectorImpl(
  nodeAttrs: NodeAttrs,
  instantTx: CurrentTx[InstantEnvKey], mainTx: CurrentTx[MainEnvKey]
) extends TxSelector with CoHandlerProvider {
  def txOf(obj: Obj) = txOf(obj(nodeAttrs.objId))
  private def txOf(objId: ObjId) = if(objId.hi==0) instantTx() else mainTx()
  def rawIndex(tx: BoundToTx) = {
    val pTx = tx.asInstanceOf[ProtectedBoundToTx[_]]
    if(pTx.enabled) pTx.rawIndex else Never()
  }
  def rawIndex(objId: ObjId) = rawIndex(txOf(objId))
  def handlers = List(CoHandler(TxSelectorKey)(this))
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
  protected def register(tx: ProtectedBoundToTx[DBEnvKey], on: Boolean) = {
    currentTx.value = if(on) Some(tx) else None
    checkAll.switchTx(tx,on)
  }
}

//*Instant, MergerMain
class DefaultTxManagerImpl[DBEnvKey](
    connectionLifeCycle: LifeCycle, env: DBEnv[DBEnvKey],
    val currentTx: CurrentTxImpl[DBEnvKey],
    val checkAll: PreCommitCheckAllOfConnection
) extends BaseTxManager[DBEnvKey] with DefaultTxManager[DBEnvKey] {
  def rwTx[R](f: () ⇒ R) = withBusy { () ⇒
    val lifeCycle = connectionLifeCycle.sub()
    val res = env.rwTx(lifeCycle){ rawIndex ⇒
      val tx = new ProtectedBoundToTx[DBEnvKey](rawIndex, true)
      register(tx,on=true)
      val res = f()
      val fails = checkAll.checkTx(currentTx())
      if(fails.nonEmpty) throw new Exception(s"$fails")
      register(tx,on=false)
      res
    }
    lifeCycle.close()
    res
  }
  def roTx[R](f: () ⇒ R) = withBusy { () ⇒
    val lifeCycle = connectionLifeCycle.sub()
    val rawIndex = env.roTx(lifeCycle)
    val tx = new ProtectedBoundToTx[DBEnvKey](rawIndex, true)
    register(tx,on=true)
    val res = f()
    register(tx,on=false)
    lifeCycle.close()
    res
  }
}

class SessionMainTxManagerImpl(
    connectionLifeCycle: LifeCycle, mainEnv: DBEnv[MainEnvKey],
    val currentTx: CurrentTxImpl[MainEnvKey],
    val checkAll: PreCommitCheckAllOfConnection,
    muxFactory: MuxFactory
) extends BaseTxManager[MainEnvKey] with SessionMainTxManager {
  class Mux(val tx: ProtectedBoundToTx[MainEnvKey], val lifeCycle: LifeCycle)
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
      val tx = new ProtectedBoundToTx[MainEnvKey](rawIndex, false)
      mux = Some(new Mux(tx, lifeCycle))
      register(tx,on=true)
    }
    mux.get.tx.enabled = true
    val res = f()
    mux.get.tx.enabled = false
    res
  }
}

