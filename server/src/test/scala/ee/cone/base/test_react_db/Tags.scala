package ee.cone.base.test_react_db

import ee.cone.base.connection_api.DictMessage
import ee.cone.base.util.Never
import ee.cone.base.vdom._
import ee.cone.base.vdom.Types.VDomKey

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

abstract class SimpleElement extends ElementValue {
  def appendJsonAttributes(builder: JsonBuilder) = ()
}
object WrappingElement extends SimpleElement { def elementType = "span" }
object DivElement extends SimpleElement { def elementType = "div" }


case class TextContentElement(content: String) extends ElementValue {
  def elementType = "span"
  def appendJsonAttributes(builder: JsonBuilder) =
    builder.append("content").append(content)
}

case class ButtonElement(caption: String)(
  val receive: PartialFunction[DictMessage,Unit]
) extends ElementValue with MessageReceiver {
  def elementType = "input"
  def appendJsonAttributes(builder: JsonBuilder) = builder
    .append("type").append("button")
    .append("value").append(caption)
    .append("onClick").append("send")
}

case class InputTextElement(value: String, deferSend: Boolean)(
  input: InputAttributes
)(
  val receive: PartialFunction[DictMessage,Unit]
) extends ElementValue with MessageReceiver {
  def elementType = "input"
  def appendJsonAttributes(builder: JsonBuilder) = {
    builder.append("type").append("text")
    input.appendJson(builder, value, deferSend)
  }
}

trait OfDiv

trait Action {
  def invoke(): Unit
}

class Tags(
  child: ChildPairFactory, inputAttributes: InputAttributes,
  onChange: OnChange, onClick: OnClick
) {
  def root(children: List[ChildPair[OfDiv]]) =
    child("root", WrappingElement, children).value
  def span(key: VDomKey, children: List[ChildPair[OfDiv]]) =
    child[OfDiv](key, WrappingElement, children)

  def text(key: VDomKey, label: String) =
    child[OfDiv](key, TextContentElement(label), Nil)
  def input(prop: AlienRef[String]) =
    child[OfDiv](prop.key, InputTextElement(prop.value, deferSend=true)(inputAttributes){
      case `onChange`(v) => prop.onChange(v)
      case _ => Never()
    }, Nil)
  def button(key: VDomKey, caption: String, action: Action) =
    child[OfDiv](key, ButtonElement(caption){
      case `onClick` => action.invoke()
    }, Nil)
}
