package io.github.rewlad.ladler.test_react

import io.github.rewlad.ladler.server.{ActionOf, ReceivedMessage}
import io.github.rewlad.ladler.util.Never
import io.github.rewlad.ladler.vdom._


abstract class SimpleElement extends ElementValue {
  def appendJsonAttributes(builder: JsonBuilder) = ()
  def handleMessage(message: ReceivedMessage) = Never()
}
object WrappingElement extends SimpleElement { def elementType = "span" }
object DivElement extends SimpleElement { def elementType = "div" }
object TableElement extends SimpleElement { def elementType = "table" }
object TrElement extends SimpleElement { def elementType = "tr" }
object TdElement extends SimpleElement { def elementType = "td" }

abstract class ButtonElement extends ElementValue {
  def elementType = "input"
  def caption: String
  def onClick(): Unit
  def appendJsonAttributes(builder: JsonBuilder) = builder
    .append("type").append("button")
    .append("value").append(caption)
    .append("onClick").append("send")

  def handleMessage(message: ReceivedMessage) = ActionOf(message) match {
    case "click" => onClick()
  }
}
case class VoidButtonElement(caption: String) extends ButtonElement {
  def onClick() = ()
}
case class ResetButtonElement(prop: StrProp) extends ButtonElement {
  def caption = "Reset"
  def onClick() = prop.set("")
}

case class InputTextElement(value: String, prop: StrProp, deferSend: Boolean) extends ElementValue {
  def elementType = "input"
  def appendJsonAttributes(builder: JsonBuilder) = {
    builder.append("type").append("text")
    Input.appendJsonAttributes(builder, value, deferSend)
  }
  def onChange(value: String): Unit = prop.set(value)
  def handleMessage(message: ReceivedMessage) =
    Input.changedValueFromMessage(message, onChange) || Never()
}

case class TextContentElement(content: String) extends ElementValue {
  def elementType = "span"
  def appendJsonAttributes(builder: JsonBuilder) =
    builder.append("content").append(content)
  def handleMessage(message: ReceivedMessage) = Never()
}

case class Anchor(href: String, content: String) extends ElementValue {
  def elementType = "a"
  def handleMessage(message: ReceivedMessage) = Never()
  def appendJsonAttributes(builder: JsonBuilder) = builder
    .append("href").append(href)
    .append("content").append(content)
}

trait StrProp {
  def get: String
  def set(value: String): Unit
}

trait OfDiv
trait OfTable
trait OfTr

object Tag {
  def root(children: List[ChildPair[OfDiv]]) =
    Child(0, WrappingElement, children).value
  def resetButton(key: Int, prop: StrProp) =
    Child[OfDiv](key, ResetButtonElement(prop))
  def inputText(key: Int, label: String, prop: StrProp, deferSend: Boolean) =
    Child[OfDiv](key, WrappingElement,
      Child[OfDiv](0, TextContentElement(label)) ::
        Child[OfDiv](1, InputTextElement(prop.get, prop, deferSend: Boolean)) ::
        Nil
    )
  def table(key: Int, children: List[ChildPair[OfTable]]) =
    Child[OfDiv](key, TableElement, children)
  def tr(key: Int, children: List[ChildPair[OfTr]]) =
    Child[OfTable](key, TrElement, children)
  def td(key: Int, children: List[ChildPair[OfDiv]]) =
    Child[OfTr](key, TdElement, children)
  def button(key: Int, value: String) =
    Child[OfDiv](key, VoidButtonElement(value))
  def anchor(key: Int, name: String, content: String) =
    Child[OfDiv](key, Anchor(name, content))
}
