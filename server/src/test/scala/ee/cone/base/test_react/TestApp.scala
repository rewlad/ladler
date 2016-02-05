package ee.cone.base.test_react

import java.nio.file.Paths

import ee.cone.base.connection_api.{ReceiverOf, DictMessage}
import ee.cone.base.server._
import ee.cone.base.vdom._

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

class TestFrameHandler(sender: SenderOfConnection, models: List[Model]) extends ReceiverOf {
  private lazy val modelChanged = new VersionObserver
  private lazy val diff = new DiffImpl(MapValueImpl)
  private lazy val dispatch = new Dispatch
  private var hashForView = ""

  def receive = {
    case message@DictMessage(mv) =>
      dispatch(message)
      for(hash <- mv.get("X-r-location-hash")) hashForView = hash
    case PeriodicMessage =>
      val view = hashForView match {
        case "big" => new BigView(models)
        case "interactive" => new InteractiveView(models)
        case _  => new IndexView
      }
      modelChanged(view.modelVersion){
        dispatch.vDom = view.generateDom
        diff.diff(dispatch.vDom).foreach(d=>sender.send("showDiff", JsonToString(d)))
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
    def createMessageReceiverOfConnection(context: ContextOfConnection) =
      new TestFrameHandler(context.sender, models)
  }
  server.start()
  println(s"SEE: http://127.0.0.1:${server.httpPort}/react-app.html")
}
