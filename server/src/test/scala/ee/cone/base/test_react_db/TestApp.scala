package ee.cone.base.test_react_db

import java.nio.file.Paths
import ee.cone.base.util.Setup
import scala.collection.immutable.SortedMap

import ee.cone.base.connection_api.{DictMessage, Message}
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

class TestTx(connection: TestConnection) extends ReceiverOf[Message] {
  /*def regenerateDom() = {
  if(unmergedReadModelIndex.nonEmpty && unmergedReadModelIndex.get.baseVersion < sharedIndex.version)
    unmergedReadModelIndex = None


  connection.mainDB.withTx(rw=false){
    connection.tempDB.withTx(rw=false){

    }

  }

  ???
}*/
  lazy val vDom = new Cached[Value] { () =>

    ???
  }
  private lazy val periodicFullReset = new OncePer(1000, reset)
  private def reset() = {
    lifeCycle.close()
    connection.tx.reset()
  }
  private var vDomDeferReset = false
  lazy val lifeCycle = connection.context.lifeCycle.sub()
  lazy val eventFactConverter = new RawFactConverterImpl(DBLayers.eventFacts, 0L)
  lazy val dbEvents = new DBEvents(connection.tempDB, lifeCycle, eventFactConverter)

  def transformMessage(path: List[String], message: DictMessage): Message = {
    val node = ResolveValue(vDom(), path)
      .getOrElse(throw new Exception(s"$path not found"))
    val transformer = node match {
      case v: MessageTransformer => v
    }
    transformer.transformMessage.lift(message).get
  }
  def receive = {
    case message@WithVDomPath(path) => receive(transformMessage(path, message))
    case ev@DBEvent(data) =>
      dbEvents.add(ev)
      vDom.reset()
    // non-db events here, vDomDeferReset = true
    // hash
    case ResetTxMessage => reset() // msg from merger can reset full context
    case PeriodicMessage =>
      periodicFullReset() // ?vDom if fail?
      if (vDomDeferReset) {
        vDomDeferReset = false
        vDom.reset()
      }
      connection.diff.diff(vDom()).foreach(d =>
        connection.context.sender.send("showDiff", JsonToString(d))
      )
  }
}


class DBEventTx(rawIndex: RawIndex, session: DBLongPairValue, mainTx: DBEventTx/*not really*/) {
  private lazy val rawFactConverter = new RawFactConverterImpl(DBLayers.eventFacts, 0L)
  private lazy val rawIndexConverter = new RawIndexConverterImpl(DBLayers.eventIndex)
  private lazy val indexed = Set[Long](SysAttrId.session)
  private lazy val innerFactIndex = new InnerFactIndex(rawFactConverter, rawIndex)
  private lazy val innerIndexIndex = new InnerIndexIndex(rawIndexConverter, rawIndex, indexed)
  private lazy val attrCalcExecutor = new AttrCalcExecutor(Nil)
  lazy val db = new RewritableTriggeringIndex(innerFactIndex, innerIndexIndex, attrCalcExecutor)
  private lazy val search = new IndexSearchImpl(rawFactConverter, rawIndexConverter, RawKeyMatcherImpl, rawIndex)
  lazy val seq = new ObjIdSequence(innerFactIndex, SysAttrId.lastObjId)

  private def delete(objId: Long) =
    search(objId).foreach{ attrId => db(objId, attrId) = DBRemoved }
  private def loadEvent(objId: Long) =
    DBEvent(search(objId).map(attrId => (attrId, db(objId, attrId))))
  private def isApplied(eventObjId: Long) =
    mainTx.search(SysAttrId.origEventObjId, DBLongValue(eventObjId)).nonEmpty
  def purgeAndLoad: List[DBEvent] = {
    val (past, future) = search(SysAttrId.session, session).partition(isApplied)
    past.foreach(delete)
    future.map(loadEvent)
  }
  def add(ev: DBEvent) = {
    val objId = seq.inc()
    db(objId, SysAttrId.session) = session
    ev.data.foreach { case (attrId, value) => db(objId, attrId) = value }
  }
}

class DBEventList(env: TestEnv, txLifeCycle: LifeCycle, session: DBLongPairValue, mainTx: DBEventTx/*not really*/){
  private var list: Option[List[DBEvent]] = None
  private def rwTx[T](f: DBEventTx=>T): T = env.rwTx(txLifeCycle) { rawIndex =>
    f(new DBEventTx(rawIndex, session, mainTx))
  }
  def get = {
    if(list.isEmpty) list = Option(rwTx(_.purgeAndLoad))
    list.get
  }
  def add(ev: DBEvent): Unit = rwTx{ tx =>
    list = Option(ev :: list.getOrElse(tx.purgeAndLoad))
    add(ev)
  }
}






case object ResetTxMessage extends Message

class ObjIdSequence(db: Index, seqAttrId: Long) {
  def last: Long = db(0L, seqAttrId) match {
    case DBRemoved => 0L
    case DBLongValue(v) => v
  }
  def last_=(value: Long) = db(0L, seqAttrId) = DBLongValue(value)
  def next = last + 1L
  def inc(): Long = {
    val res = next
    last = res
    res
  }
}

object SysAttrId {
  def lastObjId      = 0x01
  def session        = 0x02
  def origEventObjId = 0x03
}

object DBLayers {
  def eventFacts     = 2
  def eventIndex     = 3
  def readModelFacts = 4
  def readModelIndex = 5
}

case class DBEvent(data: List[(Long,DBValue)])

class TestConnection(
  val context: ContextOfConnection,
  val tempDB: TestEnv, //apply and clear on db startup
  val mainDB: TestEnv
) extends ReceiverOf[Message] {
  lazy val diff = new DiffImpl(MapValueImpl)
  lazy val tx = new Cached[TestTx](() => new TestTx(this))
  def receive = tx().receive
}

class TestEnv {
  var data = SortedMap[RawKey, RawValue]()(UnsignedBytesOrdering)
  def roTx(lifeCycle: LifeCycle): RawIndex =
    Setup(new NonEmptyUnmergedIndex) { tx =>
      data.synchronized {
        tx.data = data
      }
    }
  def rwTx[T](lifeCycle: LifeCycle)(f: RawIndex => T): T = data.synchronized {
    val tx = new NonEmptyUnmergedIndex
    tx.data = data
    val res = f(tx)
    data = tx.data
    res
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
    def createMessageReceiverOfConnection(context: ContextOfConnection) =
      new TestConnection(context, tempDB, mainDB)
  }
  server.start()
  println(s"SEE: http://127.0.0.1:${server.httpPort}/react-app.html")
}
