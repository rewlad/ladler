package ee.cone.base.test_react_db

import scala.collection.mutable.ArrayBuffer

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
    lazy val xProps =

    def createXObj(objId: Long) = new XObj(objId) {
      def xProp = xProps(objId)
      def yProp = yProps(objId)
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