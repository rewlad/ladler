package ee.cone.base.test_react_db

import ee.cone.base.connection_api.DictMessage
import ee.cone.base.util.Never
import ee.cone.base.vdom.{MessageReceiver, OnChange, OnClick, JsonBuilder, Value, InputAttributes, ChildPairFactory, ChildPair}

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

trait StrProp {
  def get: String
  def set(value: String): Unit
}

trait Action {
  def invoke(): Unit
}

class Tags(
  child: ChildPairFactory, inputAttributes: InputAttributes,
  onChange: OnChange, onClick: OnClick
) {
  def root(children: List[ChildPair[OfDiv]]) =
    child(0, WrappingElement, children).value
  def text(key: Long, label: String) =
    child[OfDiv](key, TextContentElement(label), Nil)
  def input(key: Long, prop: StrProp, deferSend: Boolean) =
    child[OfDiv](key, InputTextElement(prop.get, deferSend)(inputAttributes){
      case `onChange`(v) => prop.set(v)
      case _ => Never()
    }, Nil)
  def button(key: Long, caption: String, action: Action) =
    child[OfDiv](key, ButtonElement(caption){
      case `onClick`() => action.invoke()
    }, Nil)
}
