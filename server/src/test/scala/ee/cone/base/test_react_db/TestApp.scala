package ee.cone.base.test_react_db

import java.nio.file.Paths
import java.util.concurrent.locks.ReentrantLock
import ee.cone.base.util.{Never, Setup}
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
  def mainTx: DBAppliedTx
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
  //lazy val eventFactConverter = new RawFactConverterImpl(DBLayers.eventFacts, 0L)
  lazy val dbEventList = new DBEventList(lifeCycle, connection.tempDB, rw=true, connection.sessionId, mainTx)

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
      dbEventList.add(ev)
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



class DBDelete(db: Index, search: IndexSearch) {
  def apply(objId: Long) =
    search(objId).foreach{ attrId => db(objId, attrId) = DBRemoved }
}

class TxContext {
  def rawTx: RawTx
  class TxLayerContext(level: Long) {
    private lazy val rawIndex = rawTx.rawIndex
    private lazy val rawFactConverter =
      new RawFactConverterImpl(level*2, 0L)
    private lazy val rawIndexConverter =
      new RawIndexConverterImpl(level*2+1)
    private lazy val indexed = Set[Long](SysAttrId.sessionId) /*!!!*/
    private lazy val innerFactIndex =
      new InnerFactIndex(rawFactConverter, rawIndex)
    private lazy val innerIndexIndex =
      new InnerIndexIndex(rawIndexConverter, rawIndex, indexed)
    private lazy val attrCalcExecutor = new AttrCalcExecutor(Nil)
    protected lazy val db =
      new RewritableTriggeringIndex(innerFactIndex, innerIndexIndex, attrCalcExecutor)
    protected lazy val search =
      new IndexSearchImpl(rawFactConverter, rawIndexConverter, RawKeyMatcherImpl, rawIndex)
    protected lazy val seq = new ObjIdSequence(innerFactIndex, SysAttrId.lastObjId)
    protected lazy val delete = new DBDelete(db, search)
  }
  class EventTxLayerContext extends TxLayerContext(1L)
  class TempEventTxLayerContext(main: MainEventTxLayer) extends EventTxLayerContext {
    private lazy val dbEvents = new TempEventTxLayer(db, search, seq, delete, main)
  }
  class MainEventTxLayerContext extends EventTxLayerContext {
    lazy val applied = new MainEventTxLayer(db, search)
  }


}

class MainEventTxLayer(db: Index, search: IndexSearch) {
  def isApplied(tempEventId: Long): Boolean =
    search(SysAttrId.tempEventId, DBLongValue(tempEventId)).nonEmpty
  ???
}

class TempEventTxLayer(
  db: Index, search: IndexSearch, seq: ObjIdSequence, delete: DBDelete,
  main: MainEventTxLayer
){
  private def loadEvent(objId: Long) =
    DBEvent(search(objId).map(attrId => (attrId, db(objId, attrId))))

  def purgeAndLoad(sessionKey: String): SessionState =
    if(sessionKey.isEmpty) SessionState(loaded=true, DBRemoved, Nil)
    else search(SysAttrId.sessionKey, DBStringValue(sessionKey)) match {
      case Nil =>
        val objId = seq.inc()
        db(objId, SysAttrId.sessionKey) = DBStringValue(sessionKey)
        val sessionId = DBLongValue(objId)
        SessionState(loaded=true, sessionId, Nil)
      case objId :: Nil =>
        val sessionId = DBLongValue(objId)
        val (past, future) =
          search(SysAttrId.sessionId, sessionId).partition(main.isApplied)
        past.foreach(delete(_))
        SessionState(loaded=true, sessionId, future.map(loadEvent))
    }

  def add(state: SessionState, ev: DBEvent): SessionState = {
    if(state.sessionId == DBRemoved) Never()
    val objId = seq.inc()
    db(objId, SysAttrId.sessionId) = state.sessionId
    ev.data.foreach { case (attrId, value) => db(objId, attrId) = value }
    state.copy(eventList = ev :: state.eventList)
  }
}

case class SessionState(loaded: Boolean, sessionId: DBValue, eventList: List[DBEvent])
class MutableSessionState(
  lifeCycle: LifeCycle, env: TestEnv, rw: Boolean, sessionKey: String,
  createTxContext: (RawTx,)=>TxContext
){
  private var state = SessionState(loaded=false, DBRemoved, Nil)
  private def withTx(f: DBEventTx=>Unit): Unit = lifeCycle.sub{ mTxLifeCycle =>
    val rawTx = env.createTx(lifeCycle, rw)
    val txContext = createTxContext
    f(new DBEventTx(rawTx, mainTx))
    rawTx.commit()
  }
  private def loaded() =
    if(!state.loaded) withTx{ tx => state = tx.purgeAndLoad(sessionKey) }
  def get = { loaded(); state }
  def add(ev: DBEvent): Unit = {
    loaded()
    withTx{ tx => state = tx.add(state, ev) }
  }
}

/*
class SysProps(db: Index, indexSearch: IndexSearch) {
  class Prop(attrId: Long) {
    def apply(objId: Long) = db(objId, attrId)
    def update(objId: Long, value: DBValue) = db(objId, attrId) = value
    def search(value: DBValue) = indexSearch(attrId, value)
  }
  lazy val lastObjId      = new Prop(0x01)
  lazy val sessionId      = new Prop(0x02)
  lazy val appliedId      = new Prop(0x03)
}
*/


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
  def sessionId      = 0x02
  def sessionKey     = 0x03
  def tempEventId    = 0x04
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

/*
app
connection
snapshot / ro Tx
frame / message handling
mTx / rw Tx
*/

class RawTx(val rawIndex: RawIndex, val commit: ()=>Unit)
trait DBEnv {
  def createTx(txLifeCycle: LifeCycle, rw: Boolean): RawTx
}

class TestEnv extends DBEnv {
  private var data = SortedMap[RawKey, RawValue]()(UnsignedBytesOrdering)
  private lazy val lock = new ReentrantLock
  private def createRawIndex() = Setup(new NonEmptyUnmergedIndex) { i =>
    synchronized {
      i.data = data
    }
  }
  def createTx(txLifeCycle: LifeCycle, rw: Boolean) =
    if(!rw) new RawTx(createRawIndex(), () => ())
    else txLifeCycle.setup {
      lock.lock()
      val index = createRawIndex()
      def commit() = {
        if(!lock.isHeldByCurrentThread) Never()
        synchronized {
          data = index.data
        }
      }
      new RawTx(index, commit)
    }(_ => lock.unlock())
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
