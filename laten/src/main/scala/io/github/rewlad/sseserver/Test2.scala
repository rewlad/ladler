package io.github.rewlad.sseserver

case class SimpleAttributeKey(toStringKey: String) extends Key
object Attr {
  def apply(key: String, value: String) =
    SimpleAttributeKey(key) -> StringValue(value)
}
case class SimpleElementKey(elementType: String, key: Int) extends ElementKey
object Tag {
  def apply(tagName: String, key: Int, attributes: Map[Key,Value], elements: Seq[(ElementKey,MapValue)]): (ElementKey,MapValue) =
    SimpleElementKey(tagName,key) -> Children(attributes,elements)
}

class Test2FrameHandler(sender: SenderOfConnection) extends FrameHandler {
  private var prevVDom: Option[Value] = None
  def generateDom = {
  //lazy val generateDom = {
    val size = 100
    Children(Map(), Seq(
      Tag("table", 0, Map(), (1 to size).map(trIndex =>
        Tag("tr", trIndex, Map(), (1 to size).map(tdIndex =>
          Tag("td", tdIndex, Map(), Seq(
            Tag("input", 0, Map(
              Attr("type","button"),
              Attr("value",
                if(trIndex==25 && tdIndex==25)
                  s"${System.currentTimeMillis / 100}"
                else s"$trIndex/$tdIndex"
              )
            ), Nil)
          ))
        ))
      ))
    ))
  }

  def frame(messageOption: Option[ReceivedMessage]): Unit = {
    println(s"in  ${Thread.currentThread.getId}")
    val vDom = generateDom
    Diff(prevVDom, vDom).foreach{ diff =>
      val builder = new JsonBuilderImpl
      ToJson(builder,diff)
      sender.send("showDiff",builder.toString)
      prevVDom = Some(vDom)
      println(builder.toString)
    }
    println(s"out ${Thread.currentThread.getId}")
  }
}

object Test2App extends App {
  new SSERHttpServer {
    def threadCount = 5
    def allowOrigin = Some("*")
    def ssePort = 5556
    def httpPort = 5557
    def framePeriod = 20
    def purgePeriod = 2000
    def createFrameHandlerOfConnection(sender: SenderOfConnection) =
      new Test2FrameHandler(sender)
  }.start()
}

