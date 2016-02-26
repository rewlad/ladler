package ee.cone.base.test_react_db

import java.nio.file.Paths
import java.util.UUID
import java.util.concurrent.{TimeUnit, BlockingQueue}
import java.util.concurrent.locks.ReentrantLock


import ee.cone.base.connection_api._

import scala.collection.immutable.SortedMap

import ee.cone.base.db._
import ee.cone.base.db.Types._
import ee.cone.base.server._
import ee.cone.base.util.{Single, Never, Setup}

class LifeCacheState[C] {
  private var state: Option[C] = None
  def apply() = state
  def set(lifeCycle: LifeCycle, value: =>C) = {
    if(state.nonEmpty) Never()
    lifeCycle.setup()(_ => state = None)
    state = Option(value)
  }
}

class LifeCache[C] {
  lazy val lifeCycle = new LifeCacheState[LifeCycle]
  def apply(create: =>C): ()=>C = {
    val base = new LifeCacheState[C]
    () =>
      if(base().isEmpty) base.set(lifeCycle().get, create)
      base().get
  }
}

////

class FindOrCreateSrcId(
  srcId: Attr[UUID],
  searchSrcId: ListByValue[UUID],
  seq: ObjIdSequence
) {
  def apply(value: UUID) = Single.option(searchSrcId.list(value))
    .getOrElse(Setup(seq.inc()){ node => node(srcId) = value })
}
class ObjIdSequence(
  seqNode: DBNode, //new DBNodeImpl(0L)
  seqAttr: Attr[Option[DBNode]]
) {
  def inc(): DBNode = {
    val res = new DBNodeImpl(seqNode(seqAttr).getOrElse(seqNode).objId + 1L)
    seqNode(seqAttr) = Some(res)
    res
  }
}

////



class TestAppMix extends ServerAppMix with DBAppMix {
  lazy val httpPort = 5557
  lazy val staticRoot = Paths.get("../client/build/test")
  lazy val ssePort = 5556
  lazy val threadCount = 5
  lazy val mainDB = new TestEnv
  lazy val instantDB = new TestEnv
  lazy val createConnection =
    (lifeCycle:LifeCycle,socketOfConnection: SocketOfConnection) ⇒
      new TestConnectionMix(this, lifeCycle, socketOfConnection)
}

class TestConnectionMix(
  app: TestAppMix, val lifeCycle: LifeCycle, val socket: SocketOfConnection
) extends ServerConnectionMix with Runnable {
  lazy val serverAppMix = app
  lazy val allowOrigin = Some("*")
  lazy val framePeriod = 200
  lazy val mainDB = app.mainDB
  lazy val run = new SnapshotRunningConnection(registrar, lifeCycle, sender, createLifeCycle, mainDB, createSnapshot)
  lazy val createLifeCycle = (lifeCycle:LifeCycle) => new LifeCycleImpl(Some(lifeCycle))
  lazy val createSnapshot =
    (lifeCycle: LifeCycle, rawTx: RawTx) =>
      new TestSnapshotMix(this, lifeCycle, rawTx)
}

class TestSnapshotMix(
  connection: TestConnectionMix, val lifeCycle: LifeCycle, val rawTx: RawTx
) extends  MixBase[TxComponent] with Runnable {
  lazy val run = new TestSnapshot()
}

object TestApp extends App {
  val app = new TestAppMix
  app.start()
  println(s"SEE: http://127.0.0.1:${app.httpPort}/react-app.html")
}

////

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
    }(_=>lock.unlock())
  def start() = ()
}

// register
class TxData(connection: SnapshotRunningConnection, val lifeCycle: LifeCycle) extends Registration {
  override def open() = connection.snapshotData = Some(this)

  override def close() = connection.snapshotData = None
}

class SnapshotRunningConnection(
  registrar: Registrar[ConnectionComponent],
  connectionLifeCycle: LifeCycle,
  sender: SenderOfConnection,
  createLifeCycle: LifeCycle=>LifeCycle,
  mainEnv: DBEnv,
  createSnapshot: (LifeCycle,RawTx)=>CanStart,
  eventSourceOperations:  EventSourceOperations
) {
  var snapshotData: Option[TxData] = None
  private def needSnapshot(): TxData = {
    if(snapshotData.isEmpty) makeSnapshot()
    snapshotData.get
  }
  private def makeSnapshot(): Unit = {
    val lifeCycle = createLifeCycle(connectionLifeCycle)
    lifeCycle.open()
    val rawTx = mainEnv.createTx(lifeCycle, rw = false)
    val snapshot = createSnapshot(lifeCycle, rawTx)
    snapshot.start()
  }
  private def withEventTx(rw: Boolean)(actions: ⇒Unit): Unit = {
    val lifeCycle = createLifeCycle(needSnapshot().lifeCycle)
    lifeCycle.open()
    val rawTx = mainEnv.createTx(lifeCycle, rw = rw)
    val snapshot = createSnapshot(lifeCycle, rawTx)
    snapshot.start()
    actions
    lifeCycle.close()
  }
  var vDomData: Option[()] = None
  private def needVDom() = {
    if(vDomData.isEmpty) makeVDom()
    vDomData.get
  }

  private def makeVDom(){

    withEventTx(rw=false){
      eventSourceOperations.sessionIncrementalApply()
    }
    // gen
  }
  def apply(): Unit = try {
    registrar.register()
    while(true) {
      // may be close old Snapshot here




    }
  } catch {
    case e: Exception ⇒
      sender.send("fail", "") //todo
      throw e
  } finally {
    connectionLifeCycle.close()
  }
}

class TestSnapshot() extends TxComponent {
  def closeIfNotFresh() = {
    ???
    System.currentTimeMillis
  }
  def apply() = {

    //snapshot.init;
    while(???){ // isOpen
      // incrementalApply
      // runVDom
      // closeIfNotFresh
    }
  }
}

////

trait SessionState {
  def sessionKey: UUID
}


//! lost calc-s
//! id-ly typing

// no SessionState? no instantSession? create
// create event, req, undo, commit
// apply, checkAll
class EventSourceOperations(
  sessionState: SessionState,
  createObj: Attr[Boolean]=>DBNode,

  instantSessionsBySessionKey: ListByValue[UUID],

  isMainSessionAttr: Attr[Boolean],
  sessionIdAttr: Attr[Option[Long]],
  mainSessionsBySessionId: ListByValue[Long],

  unmergedEventsFromIdAttr: Attr[Option[Long]],
  eventsBySessionId: ListByValue[Long], // instant
  undoByEvent: ListByValue[DBNode],

  getSeqNode: ()=>DBNode,
  unmergedRequestsFromIdAttr: Attr[Option[Long]],
  requestsAll: ListByValue[Boolean]

) {
  class EventSource[Value](listByValue: ListByValue[Value], value: Value, seqRef: Ref[Option[Long]]) {
    def poll(): Option[DBNode] = {
      val eventsFromId: Long = seqRef().getOrElse(0L)
      val eventOpt = Single.option(listByValue.list(value, eventsFromId, 1L))
      if(eventOpt.isEmpty){ return None }
      seqRef() = Some(eventOpt.get.objId+1L)
      if(undoByEvent.list(eventOpt.get).isEmpty) eventOpt else poll()
    }
  }
  def applyEvents(sessionId: Long, isNotLast: DBNode=>Boolean) = {
    val seqNode = Single.option(mainSessionsBySessionId.list(sessionId)).getOrElse{
      val mainSession = createObj(isMainSessionAttr)
      mainSession(sessionIdAttr) = Some(sessionId)
      mainSession
    }
    val seqRef = seqNode(unmergedEventsFromIdAttr.ref)
    val src = new EventSource(eventsBySessionId, sessionId, seqRef)
    applyEvents(src, isNotLast)
  }
  def applyEvents(src: EventSource[Long], isNotLast: DBNode=>Boolean): Unit = {
    val eventOpt = src.poll()
    if(eventOpt.isEmpty) { return }
    ??? // apply
    if(isNotLast(eventOpt.get)) applyEvents(src, isNotLast)
  }


  def sessionIncrementalApply(): Unit = {
    val instantSession = Single(instantSessionsBySessionKey.list(sessionState.sessionKey))
    // or create?
    val sessionId = instantSession.objId
    applyEvents(sessionId, (_:DBNode)=>false)
  }
  def mergerIncrementalApply(): Unit = {
    val seqRef = getSeqNode()(unmergedRequestsFromIdAttr.ref)
    val reqSrc = new EventSource(requestsAll, true, seqRef)
    val reqOpt = reqSrc.poll()
    if(reqOpt.isEmpty) { return }
    val req = reqOpt.get
    val sessionId = req(sessionIdAttr).get
    applyEvents(sessionId, (ev:DBNode)=>
      if(ev.objId<req.objId) true else if(ev.objId==req.objId) false else Never()
    )
    // checkAll by req?
  }

}




/*

incoming: BlockingQueue[DictMessage],
framePeriod: Long,

//, keepAlive: KeepAlive, queue: BlockingQueue[DictMessage],
components: =>List[ConnectionComponent]

lazy val receivers = components.collect{ case r: ReceiverOfMessage => r }
def dispatch(message: Message) = receivers.foreach(_.receive(message))
    needFreshSnapshot()
    needFreshVDom() //show
    val message = Option(incoming.poll(framePeriod,TimeUnit.MILLISECONDS))
    dispatch(message.getOrElse(PeriodicMessage)) //dispatch // can close / set refresh time
  }*/