package io.github.rewlad.sseserver.test_feedback

import java.nio.file.Paths

import io.github.rewlad.sseserver._

case class InputKey(key: Int) extends ElementKey { def elementType = "input" }
abstract class ButtonAttributes extends AttributesValue {
  def caption: String
  def onClick(): Unit
  def appendJson(builder: JsonBuilder) =
    builder.startObject()
      .append("type").append("button")
      .append("value").append(caption)
      .append("onClick").append("send")
    .end()
  def handleMessage(message: ReceivedMessage) = ActionOf(message) match {
    case "click" => onClick()
  }
}
case class ResetButtonAttributes(prop: StrProp) extends ButtonAttributes {
  def caption = "Reset"
  def onClick() = prop.set("")
}

case class InputTextAttributes(value: String, prop: StrProp) extends AttributesValue {
  def appendJson(builder: JsonBuilder) =
    builder.startObject()
      .append("type").append("text")
      .append("value").append(value)
      .append("onChange").append("send")
    .end()
  def handleMessage(message: ReceivedMessage) = ActionOf(message) match {
    case "change" => prop.set(message.value("X-r-vdom-value"))
  }
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

object Tag {
  def resetButton(key: Int, prop: StrProp) = InputKey(key) -> ResetButtonAttributes(prop)
  def inputText(key: Int, prop: StrProp) = InputKey(key) -> InputTextAttributes(prop.get, prop)
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
  private def find(pairs: List[(Key,Value)], path: List[String]): Value = pairs match {
    case (key:ElementKey,value) :: _ if key.jsonKey == path.head => value match {
      case m: MapValue => find(m.value, path.tail)
      case v if path.isEmpty => v
    }
    case _ :: pairsTail => find(pairsTail, path)
  }
  def dispatch(messageOption: Option[ReceivedMessage]) =
    for(message <- messageOption; path <- message.value.get("X-r-vdom-path"))
      find(prevVDom.value, path.split("/").toList) match {
        case v: AttributesValue => v.handleMessage(message)
      }
}



class TestFrameHandler(sender: SenderOfConnection, model: TestModel) extends FrameHandler {
  private lazy val modelChanged = new VersionObserver(()=>model.version.toString)
  private lazy val reactiveVDom = new ReactiveVDom(sender)
  def generateDom = {
    import Tag._
    MapValue(Children(
      inputText(0,model) :: inputText(1,model) :: resetButton(2,model) :: Nil
    ))
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

