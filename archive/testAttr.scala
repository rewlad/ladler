
/*
class B
class D extends C {
    def b = new B
}
trait C {
    implicit def b: B
    def a = new A
}
class A(implicit b: B)
*/

/*
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
*/
/* WORKS, BUT ADDS 2s to compile time?*/
/*
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
*/


object Test6 {
  object SysApi {
    type ObjId = Long
    trait DB {
        def get[Label,Value](objId: ObjId, attr: Attr[Label,Value]): Value = ???
        def set[Label,Value](objId: ObjId, attrId: Attr[Label,Value], value: Value): Unit = ???
    }
    trait Converter[Value]
    case class Attr[-Label,Value](labelId: Long, propId: Long)
    case class Obj[Label](objId: ObjId)(db: DB) {
        def apply[Value](attr: Attr[Label,Value])(implicit converter: Converter[Value]) = db.get(objId, attr)
        def update[Value](attr: Attr[Label,Value], value: Value)(implicit converter: Converter[Value]) = db.set(objId, attr, value)
    }
  }
    
  
  
  
  implicit object DoubleConverter extends Converter[Double]
  
  case class Obj[Label](objId: ObjId)(db: DB) {
    def apply[Value](attr: Attr[Label,Value])(implicit converter: Converter[Value]) = db.get(objId, attr)
    def update[Value](attr: Attr[Label,Value], value: Value)(implicit converter: Converter[Value]) = db.set(objId, attr, value)
  }
  val db = new DB
  // def
  trait Point1D
  trait Point2D extends Point1D
  val xc = Attr[Point1D,Double](0,0x0005)
  val yc = Attr[Point2D,Double](0,0x0006)
  // test
  val point1D = Obj[Point1D](7)(db)
  val point2D = Obj[Point2D](10)(db)

  point1D(xc)
  point1D(xc) = 8.0
  // point1D(yc)
  // point1D(yc) = 9.0

  point2D(xc)
  point2D(xc) = 8.0
  point2D(yc)
  point2D(yc) = 9.0
}
