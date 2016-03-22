package ee.cone.base.test_react_db

import ee.cone.base.connection_api.DictMessage
import ee.cone.base.util.Never
import ee.cone.base.vdom._
import ee.cone.base.vdom.Types.VDomKey

abstract class ElementValue extends VDomValue {
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
object DivElement extends SimpleElement { def elementType = "div" }
object SpanElement extends SimpleElement { def elementType = "span" }

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

class TestTags(
  child: ChildPairFactory, inputAttributes: InputAttributes,
  onChange: OnChange, onClick: OnClick
) {
  def span(key: VDomKey, children: List[ChildPair[OfDiv]]) =
    child[OfDiv](key, SpanElement, children)
  def div(key: VDomKey, children: List[ChildPair[OfDiv]]) =
    child[OfDiv](key, DivElement, children)
  def input(key: String, value: String, change: String=>Unit) =
    child[OfDiv](key, InputTextElement(value, deferSend=true)(inputAttributes){
      case `onChange`(v) => change(v)
      case _ => Never()
    }, Nil)
  def button(key: String, caption: String, action: ()=>Unit) =
    child[OfDiv](key, ButtonElement(caption){
      case `onClick`() => action()
    }, Nil)
}