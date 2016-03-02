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
  def of[Value](create: ()=>Value) = new AliveValueImpl(this, create)
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

class AliveValueImpl[Value](lifeCycle: LifeCycle, create: ()=>Value) extends AliveValue[Value] {
  lazy val value = create()
  def onClose(doClose: Value=>Unit) = {
    lifeCycle.onClose(()=>doClose(value))
    this
  }
  def updates(set: Option[Value]=>Unit) = {
    lifeCycle.onClose(()=>set(None))
    set(Some(value))
    this
  }
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
  def list[In,Out](ev: EventKey[In,Out]): List[CoHandler[In,Out]] =
    value.getOrElse(ev,Nil).asInstanceOf[List[CoHandler[In,Out]]]
  private lazy val value: Map[EventKey[_,_], List[BaseCoHandler]] =
    createHandlers().collect { case h: CoHandler[_,_] ⇒ h.on.map(ev=>(ev:EventKey[_,_],h:BaseCoHandler)) }
      .flatten.groupBy(_._1).mapValues(_.map(_._2))
}