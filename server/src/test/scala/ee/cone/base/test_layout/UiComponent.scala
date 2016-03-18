
package ee.cone.base.test_layout

import ee.cone.base.connection_api.DictMessage
import ee.cone.base.util.Never
import ee.cone.base.vdom.Types.VDomKey
import ee.cone.base.vdom._

/**
  * Created by wregs on 2.02.16.
  */

abstract class ElementValue extends Value {
  def elementType: String
  def appendJsonAttributes(builder: JsonBuilder): Unit
  def appendJson(builder: JsonBuilder) = {
    builder.startObject()
      .append("tp").append(elementType)
    appendJsonAttributes(builder)
    builder.end()
  }
}

case class TextContentElement(content: String) extends ElementValue {
  def elementType = "span"
  def appendJsonAttributes(builder: JsonBuilder) =
    builder.append("content").append(content)
}

abstract class StyledElement(style:String,props:String="") extends ElementValue{
  def appendJsonAttributes(builder: JsonBuilder) = {
    if(!style.isEmpty) {
      builder.append("style").startObject()
      style.split(Array(':',',')).foreach(a => builder.append(a))

      builder.end()
    }

    if(!props.isEmpty) {
      props.split(Array(':',',')).foreach(a => a match {
        case "true" => builder.append(true)
        case "false"=> builder.append(false)
        case _ =>builder.append(a)
      })
    }
  }
}

case class DivElement(style:String="",props:String="") extends StyledElement(style,props){
  def elementType="div"
}

case class FlexGrid(props:String) extends StyledElement("",props){
  def elementType="FlexGrid"
}
case class FlexGridShItem(props:String) extends StyledElement("",props){
  def elementType="FlexGridShItem"
}
case class FlexGridItem(props:String) extends StyledElement("",props){
  def elementType="FlexGridItem"
}

trait OfDiv

class Tags(
  child: ChildPairFactory
) {

  def root(children: List[ChildPair[OfDiv]]) = //+
    child("root", DivElement(), children).value
  def text(key: VDomKey, text: String)=
    child[OfDiv](key, TextContentElement(text),Nil)
  def div(key:String,style:String,props:String, children:List[ChildPair[OfDiv]])=
    child[OfDiv](key,DivElement(style,props), children) //added wregs
  //def div(key:String, children:List[ChildPair[OfDiv]])= //+
  //  child[OfDiv](key,DivElement(), children) //added wregs

  def flexGrid(key: VDomKey, props: String, children: List[ChildPair[OfDiv]])= //+
    child[OfDiv](key,FlexGrid(props),children)
  def flexGridItem(key: VDomKey, props: String, children: List[ChildPair[OfDiv]])= //+
    child[OfDiv](key,FlexGridItem(props),children)
  def flexGridShItem(key: VDomKey, props: String)= //+
    child[OfDiv](key,FlexGridShItem(props),Nil)

}
