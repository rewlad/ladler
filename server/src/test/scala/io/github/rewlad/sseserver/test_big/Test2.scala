package io.github.rewlad.sseserver.test_big

import java.nio.file.Paths

import io.github.rewlad.sseserver._

abstract class SimpleElement extends ElementValue {
  def appendJsonAttributes(builder: JsonBuilder) = ()
  def handleMessage(message: ReceivedMessage) = Never()
}
object DivElement extends SimpleElement { def elementType = "div" }
object TableElement extends SimpleElement { def elementType = "table" }
object TrElement extends SimpleElement { def elementType = "tr" }
object TdElement extends SimpleElement { def elementType = "td" }

case class ButtonElement(value: String) extends ElementValue {
  def elementType = "input"
  def appendJsonAttributes(builder: JsonBuilder) = builder
      .append("type").append("button")
      .append("value").append(value)
  def handleMessage(message: ReceivedMessage): Unit = Never()
}

trait OfDiv
trait OfTable
trait OfTr

object Tag {
  def root(children: List[ChildPair[OfDiv]]) =
    Child(0, DivElement, children)
  def table(key: Int, children: List[ChildPair[OfTable]]) =
    Child[OfDiv](key, TableElement, children)
  def tr(key: Int, children: List[ChildPair[OfTr]]) =
    Child[OfTable](key, TrElement, children)
  def td(key: Int, children: List[ChildPair[OfDiv]]) =
    Child[OfTr](key, TdElement, children)
  def button(key: Int, value: String) =
    Child[OfDiv](key, ButtonElement(value))
}

class TestFrameHandler(sender: SenderOfConnection) extends FrameHandler {
  private lazy val reactiveVDom = new ReactiveVDom(sender)
  def generateDom = {
  //lazy val generateDom = {
    import Tag._
    val size = 100
    root(
      table(0, (1 to size).map(trIndex =>
        tr(trIndex, (1 to size).map(tdIndex =>
          td(tdIndex,
            button(0,
              if(trIndex==25 && tdIndex==25)
                s"${System.currentTimeMillis / 100}"
              else s"$trIndex/$tdIndex"
            ) :: Nil
          )
        ).toList)
      ).toList) :: Nil
    )
  }

  def frame(messageOption: Option[ReceivedMessage]): Unit = {
    println(s"in  ${Thread.currentThread.getId}")
    reactiveVDom.diffAndSend(generateDom.value)
    println(s"out ${Thread.currentThread.getId}")
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
      new TestFrameHandler(sender)
  }
  server.start()
  println(s"SEE: http://127.0.0.1:${server.httpPort}/app.html")
}

