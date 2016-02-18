package ee.cone.base.server



/*
object ToClose {
  def apply[T](f: LifeCycle=>T): T = {
    val lifeCycle = new LifeCycleImpl
    try f(lifeCycle) finally lifeCycle.close()
  }
}

class A {
  private lazy val tHolder = new ThreadLocal[Option[LifeCycle]] {
    override def initialValue() = None
  }
  def doTx[T](f: =>T): T = ToClose{ lifeCycle =>
    if (tHolder.get.nonEmpty) throw new Exception("nested tx not supported")
    lifeCycle.setup(tHolder)(_.remove())
    tHolder.set(Some(lifeCycle))
    f
  }
  def apply() = tHolder.get.get
}

class CacheOnce[C](calculate: ()=>C) {
  lazy val value = calculate()
  def apply(): C = value
}

class Cache[C](lifeCycle: ()=>LifeCycle, calculate: ()=>C) {
  private var value: Option[C] = None
  def apply(): C = {
    if(value.isEmpty)
      value = lifeCycle().setup(Option(calculate()))(_ => value = None)
    value.get
  }
}
*/




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

