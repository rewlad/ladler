package io.github.rewlad.sseserver

case class SimpleAttributeKey(toStringKey: String) extends Key
object Attr {
  def apply(key: String, value: String) =
    SimpleAttributeKey(key) -> StringValue(value)
}
case class SimpleElementKey(elementType: String, key: Int) extends ElementKey
object Tag {
  def apply(tagName: String, key: Int, elements: List[(ElementKey,MapValue)]): (ElementKey,MapValue) =
    SimpleElementKey(tagName,key) -> MapValue(Children(elements))

  def button(key: Int, value: String): (ElementKey,MapValue) =
    SimpleElementKey("input",key) -> MapValue(
      (SimpleAttributeKey("type")->StringValue("button")) ::
      (SimpleAttributeKey("value")->StringValue(value)) :: Nil
    )
}



class Test2FrameHandler(sender: SenderOfConnection) extends FrameHandler {
  private var prevVDom: MapValue = MapValue(Nil)
  def generateDom = {
  //lazy val generateDom = {
    val size = 100
    MapValue(Children(
      Tag("table", 0, (1 to size).map(trIndex =>
        Tag("tr", trIndex, (1 to size).map(tdIndex =>
          Tag("td", tdIndex,
            Tag.button(0,
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
      ToJson(builder,diff)
      sender.send("showDiff",builder.toString)
      prevVDom = vDom
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

