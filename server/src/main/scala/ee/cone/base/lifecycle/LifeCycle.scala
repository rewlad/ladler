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

class ExecutionManagerImpl(
  toStart: List[CanStart], threadCount: Int
) extends ExecutionManager {
  lazy val pool = Executors.newScheduledThreadPool(threadCount)
  def start() = toStart.foreach(_.start())
  def startServer(iteration: ()=>Unit) = pool.execute(ToRunnable {
    while(true) iteration()
  })
  def startConnection(setup: LifeCycle=>CoMixBase) = pool.submit(ToRunnable {
    val lifeCycle = new LifeCycleImpl(None)
    try{
      lifeCycle.open()
      val connection = setup(lifeCycle)
      try{
        while(true) Single(connection.handlerLists.list(ActivateReceiver))()
      } catch {
        case e: Exception ⇒
          connection.handlerLists.list(FailEventKey).foreach(_(e))
          throw e
      }
    } finally lifeCycle.close()
  })
}

trait AppMixBaseImpl extends AppMixBase {
  def threadCount: Int
  lazy val executionManager = new ExecutionManagerImpl(toStart, threadCount)
}
