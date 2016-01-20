package io.github.rewlad.ladler.test_react

import java.nio.file.Paths

import io.github.rewlad.ladler.connection_api.ReceivedMessage
import io.github.rewlad.ladler.server.{SSEHttpServer,
FrameHandler,SenderOfConnection}
import io.github.rewlad.ladler.vdom._

trait Model
trait View {
  def modelVersion: String
  def generateDom: Value
}

class IndexView extends View {
  def modelVersion = "index"
  def generateDom = {
    import Tag._
    root(
      anchor(0,"#big","[big]")::
        anchor(1,"#interactive","[interactive]")::
        Nil
    )
  }
}

class TestFrameHandler(sender: SenderOfConnection, models: List[Model]) extends FrameHandler {
  private lazy val modelChanged = new VersionObserver
  private lazy val diff = new DiffImpl(MapValueImpl)
  private var hashForView = ""

  def frame(messageOption: Option[ReceivedMessage]): Unit = {
    Dispatch(diff.prevVDom, messageOption)
    for(message <- messageOption; hash <- message.value.get("X-r-location-hash"))
      hashForView = hash
    val view = hashForView match {
      case "big" => new BigView(models)
      case "interactive" => new InteractiveView(models)
      case _  => new IndexView
    }
    modelChanged(view.modelVersion){
      diff.diff(view.generateDom).foreach(d=>sender.send("showDiff", JsonToString(d)))
    }
  }
}

object TestApp extends App {
  val models = new FieldModel :: Nil
  val server = new SSEHttpServer {
    def threadCount = 5
    def allowOrigin = Some("*")
    def ssePort = 5556
    def httpPort = 5557
    def framePeriod = 20
    def purgePeriod = 2000
    def staticRoot = Paths.get("../client/build/test")
    def createFrameHandlerOfConnection(sender: SenderOfConnection) =
      new TestFrameHandler(sender, models)
  }
  server.start()
  println(s"SEE: http://127.0.0.1:${server.httpPort}/react-app.html")
}
