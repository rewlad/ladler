package ee.cone.base.draft

import java.util.concurrent._

import scala.collection.concurrent.TrieMap
import scala.collection.mutable.ArrayBuffer

/*
class LifeCacheState[C] {
  private var state: Option[C] = None
  def apply() = state
  def set(lifeCycle: LifeCycle, value: =>C) = {
    if(state.nonEmpty) Never()
    lifeCycle.setup()(_ => state = None)
    state = Option(value)
  }
}

class LifeCache[C] {
  lazy val lifeCycle = new LifeCacheState[LifeCycle]
  def apply(create: =>C): ()=>C = {
    val base = new LifeCacheState[C]
    () =>
      if(base().isEmpty) base.set(lifeCycle().get, create)
      base().get
  }
}
*/

////

/*
class FindOrCreateSrcId(
  srcId: Attr[UUID],
  searchSrcId: ListByValue[UUID],
  seq: ObjIdSequence
) {
  def apply(value: UUID) = Single.option(searchSrcId.list(value))
    .getOrElse(Setup(seq.inc()){ node => node(srcId) = value })
}
*/

////

/*
trait Model
trait View {
  def modelVersion: String
  def generateDom: Value
}

class IndexView extends View {
  def modelVersion = "index"
  def generateDom = {
    import Tag._
    root(
      anchor(0,"#big","[big]")::
        anchor(1,"#interactive","[interactive]")::
        Nil
    )
  }
}
*/

// ? periodic re-snap
// ? periodicFullReset = new OncePer(1000, reset)
// notify session using db



/*
trait IA_SomeAttr {
  def someAttr: String
}
trait A_SomeAttr extends IA_SomeAttr {
  def someAttr = ???
}
class SomeObj extends A_SomeAttr
*/

/*
abstract class Abc extends IA_txLife with IA_frameLife
class SummaryWeightCalc(
  weight: RuledIndexAdapter[Option[BigDecimal]],
  searchColor: SearchByValue[Option[String],Car],//RuledIndexAdapter[Option[Color]]
  searchWeightSummary: SearchByValue[Boolean]
) extends AttrCalc {
  def recalculate(objId: ObjId) = {
    weight(Single(searchWeightSummary(true))) =
      Some(searchColorOfCar(Some("G")).flatMap(o => weight(o)).sum)

  }
  def affectedBy = searchColor.direct.ruled :: weight.ruled :: Nil
}
trait Generated {
  def color: RuledIndexAdapter[Option[String]]
  lazy val searchColor = new SearchByValueImpl(color)
  lazy val summaryWeightCalc = new SummaryWeightCalc(???,searchColor,???)
  def info = summaryWeightCalc :: searchColor :: ??? :: Nil
}
*/

object Test {
  trait Prop[Value] {
    def apply(objId: Long): Value
  }
  trait SearchBy[Value,Obj] {
    def apply(value: Value): List[Obj]
  }


  class SearchByImpl[Value,Obj](createObj: Long=>Obj) extends SearchBy[Value,Obj] {
    def apply(value: Value): List[Obj] = createObj(1L) :: Nil
  }


  trait XProp extends Prop[String] {
    def apply(objId: Long): String = "abc"
  }
  trait HasXProp { def xProp: String }
  trait YProp
  trait HasYProp { def yProp: String }




  abstract class XObj(objId: Long) extends HasXProp with HasYProp

  class XCtl(searchByXProp: SearchBy[String,XObj]){
    def test(): Unit = searchByXProp("abc").head.yProp
  }

  class XMod {
    //lazy val xProps =

    def createXObj(objId: Long) = new XObj(objId) {
      def xProp = ??? //xProps(objId)
      def yProp = ??? //yProps(objId)
    }
    lazy val searchByXProp = new SearchByImpl[String,XObj](createXObj)
    lazy val xCtl = new XCtl(searchByXProp)
  }

}


//////////////////

object Test0 {
  class Prop[Value] {
    def apply(objId: ObjId): Value = ???
  }
  class ObjId {
    def apply[Value](prop: Prop[Value]) = prop(this)
  }
  val xProp = new Prop[String]
  val xObj = new ObjId
  xProp(xObj)
  xObj(xProp) //
}

object Test1 {
  class Prop[HasProp,Value] {
    def apply(objId: HasProp): Value = ???
  }
  class Obj

  trait HasXProp
  trait HasYProp
  class XProp extends Prop[HasXProp,String]
  class YProp extends Prop[HasYProp,String]
  class XObj extends Obj with HasXProp with HasYProp
  class YObj extends Obj with HasYProp

  val xProp = new XProp
  val yProp = new YProp
  val xObj = new XObj
  val yObj = new YObj
  xProp(xObj)
  yProp(xObj)
  yProp(yObj)
  // xProp(yObj)
}

object Test2 {
  class Prop[-Obj,Value] {
    def apply(objId: Obj): Value = ???
    def update(objId: Obj, value: Value): Unit = ???
  }
  class Obj {
    def apply[Value](prop: Prop[this.type,Value]) = prop(this)
    def update[Value](prop: Prop[this.type,Value], value: Value) = prop(this) = value
  }

  class YObj extends Obj
  class YProp extends Prop[YObj,String]

  class XObj extends YObj
  class XProp extends Prop[XObj,String]
  class XProp2 extends Prop[XObj,String]

  val yObj = new YObj
  val xObj = new XObj
  val xProp = new XProp
  val xProp2 = new XProp2
  val yProp = new YProp


  xObj(xProp)
  xObj(xProp2) = ""
  xObj(yProp)
  yObj(yProp)
  // yObj(xProp) = ""
}

object Test3 {
  val a = new ArrayBuffer[Int]
  a.update(6,9)
}

object Test5 {
  type ObjId = Long

  class DB {
    def get[Prop,Value](objId: ObjId, attr: Attr[Prop,Value]): Value = ???
    def set[Prop,Value](objId: ObjId, attrId: Attr[Prop,Value], value: Value): Unit = ???
  }
  case class Attr[Prop,Value](labelId: Long, propId: Long)
  case class Obj[Label](objId: ObjId)(db: DB) {
    def apply[Prop,Value](attr: Attr[Prop,Value])(implicit grant: CanRead[Label,Prop]): Value = db.get(objId, attr)
    def update[Prop,Value](attr: Attr[Prop,Value], value: Value)(implicit grant: CanWrite[Label,Prop]) = db.set(objId, attr, value)
  }

  trait CanRead[Label,Prop]
  trait CanWrite[Label,Prop]

  ////

  trait Point
  trait XCoordinate
  trait YCoordinate
  implicit object Grant0 extends CanWrite[Point,XCoordinate]


  val db = new DB
  val xAttr = Attr[XCoordinate,Int](0,0x0079)

  val somePoint = Obj[Point](8)(db)

  somePoint(xAttr) = 67

}

object Test6 {
  object SysInterfaces {
    type ObjId = Long
    trait DB {
      def get[Label,Value](objId: ObjId, attr: Attr[Label,Value]): Value
      def set[Label,Value](objId: ObjId, attrId: Attr[Label,Value], value: Value): Unit
    }
    trait Converter[Value]
    case class Attr[-Label,Value](labelId: Long, propId: Long)
    trait Obj[Label] {
      def objId: ObjId
      def apply[Value](attr: Attr[Label,Value])(implicit converter: Converter[Value]): Value
      def update[Value](attr: Attr[Label,Value], value: Value)(implicit converter: Converter[Value]): Unit
    }
  }
  object SysComponents {
    import SysInterfaces._
    case class ObjImpl[Label](objId: ObjId)(db: DB) extends Obj[Label] {
      def apply[Value](attr: Attr[Label,Value])(implicit converter: Converter[Value]) = db.get(objId, attr)
      def update[Value](attr: Attr[Label,Value], value: Value)(implicit converter: Converter[Value]) = db.set(objId, attr, value)
    }
    object DoubleConverter extends Converter[Double]
    class DBImpl extends DB {
      def get[Label, Value](objId: ObjId, attr: Attr[Label, Value]): Value = ???
      def set[Label, Value](objId: ObjId, attrId: Attr[Label, Value], value: Value): Unit = ???
    }
  }
  object CustomInterfaces {
    import SysInterfaces._
    trait Point1D
    trait Point2D extends Point1D
    val xc = Attr[Point1D,Double](0,0x0005)
    val yc = Attr[Point2D,Double](0,0x0006)
  }
  object CustomComponent{
    import CustomInterfaces._
    import SysInterfaces._
    def test(
        point1D: Obj[Point1D],
        point2D: Obj[Point2D]
    )(implicit converter: Converter[Double]) {
      point1D(xc)
      point1D(xc) = 8.0
      // point1D(yc)
      // point1D(yc) = 9.0


      point2D(xc)
      point2D(xc) = 8.0
      point2D(yc)
      point2D(yc) = 9.0
    }
  }

  object Mix {
    import CustomComponent._
    import CustomInterfaces._
    import SysComponents._
    val db = new DBImpl
    val point1D = ObjImpl[Point1D](7L)(db)
    val point2D = ObjImpl[Point2D](10L)(db)
    test(point1D, point2D)(DoubleConverter)
  }
}


//u can't do val

object Test7 {
  object SysInterfaces {
    trait MetaAttr
    trait Attr[-Label,Value]
    trait AttrValueType[Value] {
      def apply[Label](metaAttr: ()⇒List[MetaAttr]): Attr[Label,Value]
    }
    trait Obj[Label] {
      def apply[Value](attr: Attr[Label,Value]): Value
      def update[Value](attr: Attr[Label,Value], value: Value): Unit
    }
  }

  object CustomComponent0 {
    import SysInterfaces._
    trait Point1D
    trait Point2D extends Point1D
    class CustomComponent(
      doubleAttr: AttrValueType[Double],
      attrCaption: String⇒MetaAttr
    ) {
      val xc = doubleAttr[Point1D](()⇒
        attrCaption("xc")::
        Nil
      )
      val yc = doubleAttr[Point2D](Nil)

      def test(
        a: Obj[Point1D],
        b: Obj[Point2D]
      ) = {
        a(xc) = b(xc)
        b(yc) = 8.0
        b(xc) = 8.0
      }

    }
  }

  ////

  object StuffStuff {
    import SysInterfaces._
    trait Stuff {
      def doubleAttr: AttrValueType[Double]
      def attrId: String ⇒ MetaAttr
      def attrCaption: String ⇒ MetaAttr
      def updateByEvent: MetaAttr
      def updateByCalculation: MetaAttr
    }
  }

  object CustomInterfaces1 {
    import SysInterfaces._
    trait Point1D
    trait Point2D extends Point1D
    trait CustomAttributes {
      def xc: Attr[Point1D,Double]
      def yc: Attr[Point2D,Double]
    }
  }

  object CustomComponent1_0 {
    import StuffStuff._
    import SysInterfaces._
    import CustomInterfaces1._
    class CustomAttributesImpl(
      stuff: Stuff
    ) extends CustomAttributes {
      import stuff._
      // searchIndex, obj
      val xc = doubleAttr(()⇒List(
        attrId("7c120f8b-8104-4318-be6b-8b0dc421bfa9"),
        attrCaption("xc"),
        updateByEvent
        //options(...), afterUpdate, getValue
      ))
      val yc = doubleAttr(()⇒List(
        attrId("a9134907-58dc-4925-a481-b79d6cab1039"),
        attrCaption("yc"),
        updateByCalculation
      ))
    }

  }

  object CustomComponent1_1{
    import CustomInterfaces1._
    import SysInterfaces._
    def test(
      customAttributes: CustomAttributes,
      point1D: Obj[Point1D],
      point2D: Obj[Point2D]
    ) {
      import customAttributes._
      point1D()
      point1D(xc) = 8.0
      // point1D(yc)
      // point1D(yc) = 9.0


      point2D(xc)
      point2D(xc) = 8.0
      point2D(yc)
      point2D(yc) = 9.0
    }
  }
}

// multi allow
// gives req life
/*

def selectFromDict[A,B](a: => = ) = attr.obj[A, B](
  givesLifeToValue :: requiresLifeOfValue :: a
)

val bodyColor = attr.obj[Car,Color](List(
  givesLifeToValue, requiresLifeOfValue
))
val seatColor = attr.obj[Car,Color](List(
  givesLifeToValue, requiresLifeOfValue
))

val bodyColoredCars: Attr[Color,List[Obj[Car]]] = reverseAttr(bodyColor)

 */

//    val someParent = objAttr(List(
//      ...
//      takesLifeFromValue | requiresLifeOfValue
//    ))
// ordered chain rel (tran-s?):
//    val someRel = objChain[SomeParent,SomeChild](attr(),attr(),attr())
//    trait ObjChain[Parent,Child] {
//      parent: Attr[Child,Obj[Parent]]
//      next: Attr[Child,Child]
//      children: Attr[Parent,Seq[Child]]
//    }
//

//////////////////////////////////////////////


// System.identityHashCode(obj)
//private lazy val pool = Executors.newScheduledThreadPool(threadCount)
class Cache(pool: ScheduledExecutorService) {
  private val data = TrieMap[_,Future[_]]()
  private def toCallable[V](f: ()⇒V) = new Callable[V]{ def call() = f() }
  private def update[K,V](f: K⇒V, k: K): Future[V] = pool.submit(toCallable(()⇒{
    val startTime = System.currentTimeMillis
    val res = f(k)
    val delay = (System.currentTimeMillis-startTime)*10
    pool.schedule(toCallable(() ⇒ data -= k), delay, TimeUnit.MILLISECONDS)
    res
  }))
  def apply[K,V](f: K⇒V)(k: K) =
    data.getOrElseUpdate(k, update(f,k)).asInstanceOf[Future[V]]
}

case class A(b: Int)

object A {
  val theCache = new Cache(Executors.newScheduledThreadPool(threadCount))

  def calc(a: Int) = a + 1
  val calcCache: (Int) ⇒ Future[Int] = theCache(calc)

  val a = calcCache(8)
  val b = calcCache(9)
  a.get() + b.get()

}



("0x5689",_.abc, _.copy(abc=_))



def reduce(world, events): world = {


  val changedAbc = makeIndex(world.abc, world.efg, nextEfg, changedEfg)
  val nextAbc = if(changedAbc.isEmpty) world.abc else world.abc ++ changedAbc

}