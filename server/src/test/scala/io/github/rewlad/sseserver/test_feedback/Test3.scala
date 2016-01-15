package io.github.rewlad.sseserver.test_feedback

import java.nio.file.Paths
import java.util.Base64

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
    builder.append("value").append(value)
    if(deferSend){
      builder.append("onChange").append("local")
      builder.append("onBlur").append("send")
    } else builder.append("onChange").append("send")
  }

  def handleMessage(message: ReceivedMessage) = ActionOf(message) match {
    case "change" =>
      prop.set(UTF8String(Base64.getDecoder.decode(message.value("X-r-vdom-value-base64"))))
  }
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
  def set(value: String)
}

case class TestModel() extends StrProp {
  private var _value: String = ""
  private var _version: Int = 0
  def version = synchronized(_version)
  def get: String = synchronized(_value)
  def set(value: String): Unit = synchronized{ _value = value; _version += 1 }
}

trait ChildOfDiv
object Tag {
  def resetButton(key: Int, prop: StrProp) =
    Child[ChildOfDiv](key, ResetButtonElement(prop))
  def inputText(key: Int, label: String, prop: StrProp, deferSend: Boolean) =
    Child[ChildOfDiv](key, WithChildren(WrappingElement,
      Child[ChildOfDiv](0, TextContentElement(label)) ::
      Child[ChildOfDiv](1, InputTextElement(prop.get, prop, deferSend: Boolean)) ::
      Nil
    ))
}

class VersionObserver(version: ()=>String) {
  private var prevVer: Option[String] = None
  def thenDo(f: =>Unit): Unit = {
    val nextVer = Some(version())
    if(prevVer.isEmpty || prevVer != nextVer){
      f
      prevVer = nextVer
    }
  }
}

class ReactiveVDom(sender: SenderOfConnection){
  private var prevVDom: MapValue = MapValue(Nil)
  def diffAndSend(vDom: MapValue) = {
    Diff(prevVDom, vDom).foreach { diff =>
      val builder = new JsonBuilderImpl
      diff.appendJson(builder)
      sender.send("showDiff", builder.toString)
      println(builder.toString)
    }
    prevVDom = vDom
  }
  private def find(mapValue: MapValue, path: List[String]): Value =
    mapValue.value.collectFirst{
      case pair if pair.jsonKey == path.head => pair.value
    }.collect{
      case m: MapValue => find(m, path.tail)
      case v if path.tail.isEmpty => v
    }.getOrElse(
      throw new Exception(s"path ($path) was not found in branch ($mapValue) ")
    )

  def dispatch(messageOption: Option[ReceivedMessage]) =
    for(message <- messageOption; path <- message.value.get("X-r-vdom-path")){
      println(s"path ($path)")
      val "" :: parts = path.split("/").toList
      val attrs = find(prevVDom, parts) match { case v: ElementValue => v }
      attrs.handleMessage(message)
    }
}

class TestFrameHandler(sender: SenderOfConnection, model: TestModel) extends FrameHandler {
  private lazy val modelChanged = new VersionObserver(()=>model.version.toString)
  private lazy val reactiveVDom = new ReactiveVDom(sender)
  def generateDom = {
    import Tag._
    WithChildren(WrappingElement,
      inputText(0, "send on change", model, deferSend=false) ::
        inputText(1, "send on blur", model, deferSend=true) ::
        resetButton(2,model) :: Nil
    )
  }
  def frame(messageOption: Option[ReceivedMessage]): Unit = {
    reactiveVDom.dispatch(messageOption)
    modelChanged.thenDo{
      reactiveVDom.diffAndSend(generateDom)
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

