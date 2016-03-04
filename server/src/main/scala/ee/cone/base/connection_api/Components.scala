package ee.cone.base.connection_api

import ee.cone.base.util.Setup

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

////

trait AppMixBase extends CanStart {
  def toStart: List[CanStart] = Nil
  def start() = toStart.foreach(_.start())
}

////
trait CoMixBase extends CoHandlerProvider {
  def handlers: List[BaseCoHandler] = Nil
  lazy val handlerLists: CoHandlerLists = new CoHandlerListsImpl(()⇒handlers)
}

class CoHandlerListsImpl(createHandlers: ()=>List[BaseCoHandler]) extends CoHandlerLists {
  def list[In,Out](ev: EventKey[In,Out]): List[In=>Out] =
    value.getOrElse(ev,Nil).asInstanceOf[List[In=>Out]]
  private lazy val value = createHandlers().map{ case h: CoHandler[_,_] ⇒ h }
      .groupBy(_.on).mapValues(_.map(_.handle))
}