package ee.cone.base.test_react_db

import java.nio.file.Paths

import scala.collection.immutable.SortedMap

import ee.cone.base.connection_api.Message
import ee.cone.base.db._
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

class Cached[T](create: ()=>T){
  private var value: Option[T] = None
  def reset() = value = None
  def apply() = {
    if(value.isEmpty) value = Option(create())
    value.get
  }
}

class TestFrameHandler(
  sender: SenderOfConnection,
  tempDB: TestEnv, //apply and clear on db startup
  mainDB: TestEnv
) extends ReceiverOf[Message] {
  //private lazy val modelChanged = new VersionObserver
  private lazy val diff = new DiffImpl(MapValueImpl)
  private lazy val periodicFullReset = new OncePer(1000, reset)
  def reset() = {
    ???
    vDom.reset()
  }
  def regenerateDom() = {/*
    if(unmergedReadModelIndex.nonEmpty && unmergedReadModelIndex.get.baseVersion < sharedIndex.version)
      unmergedReadModelIndex = None
    */

    mainDB.withTx(rw=false){
      tempDB.withTx(rw=false){

      }

    }

    ???
  }

  private lazy val vDom = new Cached[Value](regenerateDom)
  private var vDomDeferReset = false

  def receive = {
    case message@WithVDomPath(path) => {
      val node = ResolveValue(vDom(), path)
        .getOrElse(throw new Exception(s"$path not found"))
      val transformer = node match { case v: MessageTransformer => v }
      val transformed = transformer.transformMessage.lift(message).get
      transformed match {
        case ev@DBEvent(data) =>
          tempDB.withTx(rw=true) { rawIndex =>
            DBEvents.write(rawIndex, ev)
          }
          vDom.reset()
        // non-db events here, vDomDeferReset = true
        // can reset full context?
      }
    }
    // hash
    case PeriodicMessage =>
      if(vDomDeferReset){
        vDomDeferReset = false
        vDom.reset()
      }
      periodicFullReset()
      // ?vDom if fail?
      diff.diff(vDom()).foreach(d => sender.send("showDiff", JsonToString(d)))
  }
}

object DBEvents {
  lazy val converter = new RawFactConverterImpl(DBLayers.unmergedEventFacts,0L)
  def copy(fromRawIndex: RawIndex, toRawIndex: RawIndex) = {
    val toIndex =
    new AllFactExtractor(converter, RawKeyMatcherImpl, toIndex)
  }
  def write(rawIndex: RawIndex, ev: DBEvent) = {
    val inner = new InnerFactIndex(converter,rawIndex)
    val db = new AppendOnlyIndex(inner)
    val objId = new ObjIdSequence(inner, SysAttrId.lastObjId).next()
    ev.data.foreach{ case (attrId, value) => db(objId, attrId) = value }
  }
}

class ObjIdSequence(db: Index, seqAttrId: Long) {
  def last: Long = db(0L, seqAttrId) match {
    case DBRemoved => 0L
    case DBLongValue(v) => v
  }
  def last_=(value: Long) = db(0L, seqAttrId) = DBLongValue(value)
  def next(): Long = {
    val res = last + 1L
    last = res
    res
  }
}

object SysAttrId {
  def lastObjId = 0x01
}

object DBLayers {
  def mergedEventFacts   = 2
  def unmergedEventFacts = 4
  def unmergedEventIndex = 5
  def readModelFacts     = 6
  def readModelIndex     = 7
}

case class DBEvent(data: List[(Long,DBValue)])
    /*

    val view = hashForView match {
      case "big" => new BigView(models)
      case "interactive" => new InteractiveView(models)
      case _  => new IndexView
    }
    modelChanged(view.modelVersion){
      dispatch.vDom = view.generateDom

    }*/





class TestEnv {
  var data = SortedMap[RawKey, RawValue]()(UnsignedBytesOrdering)
  def withTx[T](rw: Boolean)(f: RawIndex=>T):T = {
    val tx = new NonEmptyUnmergedIndex
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
  val tempDB = new TestEnv
  val mainDB = new TestEnv
  val server = new SSEHttpServer {
    def threadCount = 5
    def allowOrigin = Some("*")
    def ssePort = 5556
    def httpPort = 5557
    def framePeriod = 20
    def purgePeriod = 2000
    def staticRoot = Paths.get("../client/build/test")
    def createMessageReceiverOfConnection(sender: SenderOfConnection) =
      new TestFrameHandler(sender, tempDB, mainDB)
  }
  server.start()
  println(s"SEE: http://127.0.0.1:${server.httpPort}/react-app.html")
}
