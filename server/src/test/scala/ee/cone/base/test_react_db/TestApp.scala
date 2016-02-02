package ee.cone.base.test_react_db

import java.nio.file.Paths

import scala.collection.immutable.SortedMap

import ee.cone.base.connection_api.ReceivedMessage
import ee.cone.base.db.{RawTx, UnsignedBytesOrdering, NonEmptyUnmergedTx}
import ee.cone.base.db.Types.{RawValue, RawKey}
import ee.cone.base.server._
import ee.cone.base.vdom._

/*
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
*/
class TestFrameHandler(
  sender: SenderOfConnection,
  unmergedEventDB: TestEnv,
  mergedDB: TestEnv
) extends FrameHandler {
  //private lazy val modelChanged = new VersionObserver
  private lazy val diff = new DiffImpl(MapValueImpl)
  //private var hashForView = ""



  def frame(messageOption: Option[ReceivedMessage]): Unit = {
    Dispatch(diff.prevVDom, messageOption)
    /*
    for(message <- messageOption; hash <- message.value.get("X-r-location-hash"))
      hashForView = hash
    val view = hashForView match {
      case "big" => new BigView(models)
      case "interactive" => new InteractiveView(models)
      case _  => new IndexView
    }
    modelChanged(view.modelVersion){
      diff.diff(view.generateDom).foreach(d=>sender.send("showDiff", JsonToString(d)))
    }*/
  }
}



class TestEnv {
  var data = SortedMap[RawKey, RawValue]()(UnsignedBytesOrdering)
  def withTx[T](rw: Boolean)(f: RawTx=>T):T = {
    val tx = new NonEmptyUnmergedTx
    if(rw){
      data.synchronized{
        tx.data = data
        val res = f(tx)
        data = tx.data
        res
      }
    } else {
      data.synchronized{
        tx.data = data
      }
      f(tx)
    }

  }
}

object TestApp extends App {
  val mergedDB = new TestEnv
  val unmergedEventDB = new TestEnv
  val server = new SSEHttpServer {
    def threadCount = 5
    def allowOrigin = Some("*")
    def ssePort = 5556
    def httpPort = 5557
    def framePeriod = 20
    def purgePeriod = 2000
    def staticRoot = Paths.get("../client/build/test")
    def createFrameHandlerOfConnection(sender: SenderOfConnection) =
      new TestFrameHandler(sender, unmergedEventDB, mergedDB)
  }
  server.start()
  println(s"SEE: http://127.0.0.1:${server.httpPort}/react-app.html")
}
