package ee.cone.base.lifecycle

import java.util.concurrent.Executors

import ee.cone.base.connection_api._
import ee.cone.base.util._

sealed trait LifeStatus
case object OpenableLifeStatus extends LifeStatus
class OpenLifeStatus(val toClose: List[()=>Unit]) extends LifeStatus
case object ClosingLifeStatus extends LifeStatus
class LifeCycleImpl(parent: Option[LifeCycle]) extends LifeCycle {
  protected var status: LifeStatus = OpenableLifeStatus
  def onClose(doClose: ()=>Unit): Unit = status match { // we must not create until status is ok
    case st: OpenLifeStatus => status = new OpenLifeStatus(doClose :: st.toClose)
    case st => throw new Exception(s"$st")
  }
  def open() = status match {
    case OpenableLifeStatus =>
      parent.foreach(p=>p.onClose(close))
      status = new OpenLifeStatus(Nil)
    case st => throw new Exception(s"$st")
  }
  def close() = status match {
    case st: OpenLifeStatus =>
      status = ClosingLifeStatus
      DoClose(st.toClose)
    case _ => ()
  }
  def sub() = Setup(new LifeCycleImpl(Some(this)))(_.open())
}

object DoClose {
  def apply(toClose: List[()=>Unit]): Unit = toClose match {
    case Nil => ()
    case head :: tail => try head() finally apply(tail)
  }
}

class ExecutionManagerImpl(threadCount: Int) extends ExecutionManager {
  lazy val pool = Executors.newScheduledThreadPool(threadCount)
  def submit(f: ()=>Unit) = pool.submit(new Runnable() {
    def run() = f()
  })
  def startConnection(setup: LifeCycle=>CoMixBase) = submit { ()⇒
    val lifeCycle = new LifeCycleImpl(None)
    try{
      lifeCycle.open()
      val connection = setup(lifeCycle)
      try{
        while(true) connection.handlerLists.single(ActivateReceiver)()
      } catch {
        case e: Exception ⇒
          //println(e)
          e.printStackTrace()
          connection.handlerLists.list(FailEventKey).foreach(_(e))
          throw e
      }
    } finally lifeCycle.close()
  }
}
