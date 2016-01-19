package io.github.rewlad.ladler.server

sealed trait LifeStatus
case object OpenableLifeStatus extends LifeStatus
class OpenLifeStatus(val toClose: List[()=>Unit]) extends LifeStatus
case object ClosingLifeStatus extends LifeStatus
class LifeTime {
  protected var status: LifeStatus = OpenableLifeStatus
  def setup[C](create: =>C)(close: C=>Unit): C = status match {
    case st: OpenLifeStatus =>
      val res = create
      status = new OpenLifeStatus((()=>close(res)) :: st.toClose)
      res
    case st => throw new Exception(s"$st")
  }
  def open() = status match {
    case OpenableLifeStatus => status = new OpenLifeStatus(Nil)
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

/*
import scala.collection.mutable
import scala.reflect.ClassTag

trait Component
class Context(create: Context=>List[Component]) {
  private lazy val components = create(this)
  private lazy val byClassName = mutable.Map[String,List[Component]]() // ++ list.groupBy(_.getClass.getName)
  def list[C<:Component](implicit ct: ClassTag[C]): List[C] = {
    val cl = ct.runtimeClass
    byClassName.getOrElseUpdate(cl.getName, components.filter(cl.isInstance))
      .asInstanceOf[List[C]]
  }
  def apply[C<:Component](implicit ct: ClassTag[C]) = Single(list[C])
}
*/
////

////
/*
class LifeState[V](lifeTime: LifeTime, create: ()=>V, open: V=>Unit, close: V=>Unit) {
  private var state: Option[V] = None
  def apply(): V = {
    if(state.isEmpty) state = Some(setup())
    state.get
  }
  private def setup(): V = {
    lifeTime.add(() => state = None)
    val res = create()
    lifeTime.add(() => close(res))
    open(res)
    res
  }
}
object WithLifeTime {
  def apply[T](lifeTime: LifeTime)(body: =>T): T = try
    lifeTime.open()
    body
  finally lifeTime.close()
}

object ConnectionState {
  def apply[C](ctx: Context)(create: =>C)(open: C=>Unit = _=>())(close: C=>Unit = _=>()) =
    new LifeState[C](ctx[ConnectionLifeTime], ()=>create, open, close)
}
*/

