package ee.cone.base.test_react_db



import scala.collection.mutable.ArrayBuffer

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
    import SysInterfaces._
    import CustomInterfaces._
    def test(
        point1D: Obj[Point1D],
        point2D: Obj[Point2D]
    ) {
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
    import SysComponents._
    import CustomInterfaces._
    import CustomComponent._
    val db = new DBImpl
    val point1D = ObjImpl[Point1D](7L)(db)
    val point2D = ObjImpl[Point2D](10L)(db)
    test(point1D, point2D)
  }
}