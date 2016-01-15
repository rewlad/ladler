package io.github.rewlad.sseserver.test_feedback

import java.nio.file.Paths

import io.github.rewlad.sseserver._

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

object WrappingElement extends ElementValue {
  def elementType = "span"
  def appendJsonAttributes(builder: JsonBuilder) = ()
  def handleMessage(message: ReceivedMessage) = Never()
}
case class TextContentElement(content: String) extends ElementValue {
  def elementType = "span"
  def appendJsonAttributes(builder: JsonBuilder) =
    builder.append("content").append(content)
  def handleMessage(message: ReceivedMessage) = Never()
}

trait StrProp {
  def get: String
  def set(value: String): Unit
}

case class TestModel() extends StrProp {
  private var _value: String = ""
  private var _version: Int = 0
  def version = synchronized(_version)
  def get: String = synchronized(_value)
  def set(value: String): Unit = synchronized{ _value = value; _version += 1 }
}

trait OfDiv
object Tag {
  def root(children: List[ChildPair[OfDiv]]) =
    Child(0, WrappingElement, children)
  def resetButton(key: Int, prop: StrProp) =
    Child[OfDiv](key, ResetButtonElement(prop))
  def inputText(key: Int, label: String, prop: StrProp, deferSend: Boolean) =
    Child[OfDiv](key, WrappingElement,
      Child[OfDiv](0, TextContentElement(label)) ::
      Child[OfDiv](1, InputTextElement(prop.get, prop, deferSend: Boolean)) ::
      Nil
    )
}

class TestFrameHandler(sender: SenderOfConnection, model: TestModel) extends FrameHandler {
  private lazy val modelChanged = new VersionObserver(()=>model.version.toString)
  private lazy val reactiveVDom = new ReactiveVDom(sender)
  def generateDom = {
    import Tag._
    root(
      inputText(0, "send on change", model, deferSend=false) ::
        inputText(1, "send on blur", model, deferSend=true) ::
        resetButton(2,model) :: Nil
    )
  }
  def frame(messageOption: Option[ReceivedMessage]): Unit = {
    reactiveVDom.dispatch(messageOption)
    modelChanged.thenDo{
      reactiveVDom.diffAndSend(generateDom.value)
    }
  }
}

object TestApp extends App {
  val server = new SSERHttpServer {
    def threadCount = 5
    def allowOrigin = Some("*")
    def ssePort = 5556
    def httpPort = 5557
    def framePeriod = 20
    def purgePeriod = 2000
    def staticRoot = Paths.get("../client/build/test")
    def createFrameHandlerOfConnection(sender: SenderOfConnection) =
      new TestFrameHandler(sender, model)
    private lazy val model = TestModel()
  }
  server.start()
  println(s"SEE: http://127.0.0.1:${server.httpPort}/app.html")
}

