package io.github.rewlad.sseserver

import java.nio.file.Paths

case class TableKey(key: Int) extends ElementKey { def elementType = "table" }
case class TrKey(key: Int) extends ElementKey { def elementType = "tr" }
case class TdKey(key: Int) extends ElementKey { def elementType = "td" }
case class InputKey(key: Int) extends ElementKey { def elementType = "input" }
case class ButtonAttributes(value: String) extends Value {
  def appendJson(builder: JsonBuilder) = {
    builder.startObject()
    builder.append("type")
    builder.append("button")
    builder.append("value")
    builder.append(value)
    builder.end()
  }
}
object Tag {
  def table(key: Int, children: List[(TrKey,Value)]) = TableKey(key) -> MapValue(Children(children))
  def tr(key: Int, children: List[(TdKey,Value)]) = TrKey(key) -> MapValue(Children(children))
  def td(key: Int, children: List[(ElementKey,Value)]) = TdKey(key) -> MapValue(Children(children))
  def button(key: Int, value: String) = InputKey(key) -> ButtonAttributes(value)
}

class Test2FrameHandler(sender: SenderOfConnection) extends FrameHandler {
  private var prevVDom: MapValue = MapValue(Nil)
  def generateDom = {
  //lazy val generateDom = {
    import Tag._
    val size = 100
    MapValue(Children(
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
    ))
  }

  def frame(messageOption: Option[ReceivedMessage]): Unit = {
    println(s"in  ${Thread.currentThread.getId}")
    val vDom = generateDom
    Diff(prevVDom, vDom).foreach{ diff =>
      val builder = new JsonBuilderImpl
      diff.appendJson(builder)
      sender.send("showDiff",builder.toString)
      prevVDom = vDom
      println(builder.toString)
    }
    println(s"out ${Thread.currentThread.getId}")
  }
}

object TestBigApp extends App {
  val server = new SSERHttpServer {
    def threadCount = 5
    def allowOrigin = Some("*")
    def ssePort = 5556
    def httpPort = 5557
    def framePeriod = 20
    def purgePeriod = 2000
    def staticRoot = Paths.get("../client/build/test")
    def createFrameHandlerOfConnection(sender: SenderOfConnection) =
      new Test2FrameHandler(sender)
  }
  server.start()
  println(s"SEE: http://127.0.0.1:${server.httpPort}/app.html")
}

