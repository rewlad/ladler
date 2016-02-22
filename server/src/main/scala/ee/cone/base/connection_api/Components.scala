package ee.cone.base.connection_api

sealed trait LifeStatus
case object OpenableLifeStatus extends LifeStatus
class OpenLifeStatus(val toClose: List[()=>Unit]) extends LifeStatus
case object ClosingLifeStatus extends LifeStatus
class LifeCycleImpl(parent: Option[LifeCycle]) extends LifeCycle {
  protected var status: LifeStatus = OpenableLifeStatus
  def setup[C](create: =>C)(close: C=>Unit): C = status match {
    case st: OpenLifeStatus => // we must not create until status is ok
      val res = create
      status = new OpenLifeStatus((()=>close(res)) :: st.toClose)
      res
    case st => throw new Exception(s"$st")
  }
  def open() = status match {
    case OpenableLifeStatus =>
      parent.foreach(p=>p.setup(this)(_.close()))
      status = new OpenLifeStatus(Nil)
    case st => throw new Exception(s"$st")
  }
  def close() = status match {
    case st: OpenLifeStatus =>
      status = ClosingLifeStatus
      DoClose(st.toClose)
    case _ => ()
  }
}

object DoClose {
  def apply(toClose: List[()=>Unit]): Unit = toClose match {
    case Nil => ()
    case head :: tail => try head() finally apply(tail)
  }
}

////

trait MixBase[Component] {
  def createComponents(): List[Component] = Nil
  lazy val components = createComponents()
}

trait AppMixBase extends MixBase[AppComponent] with CanStart {
  def start() = {
    components.collect{ case c: CanStart => c.start() }
  }
}

class Registrar[Component](lifeCycle: LifeCycle, components: =>List[Component]){
  def register() = components.collect{ case r: Registration =>
    lifeCycle.setup(r)(_.close()).open()
  }
}