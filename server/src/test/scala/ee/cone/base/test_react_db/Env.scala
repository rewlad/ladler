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
  lazy val createLifeCycle = () => new LifeCycleImpl(Some(lifeCycle))
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

class SnapshotRunningConnection(
  registrar: Registrar[ConnectionComponent],
  lifeCycle: LifeCycle,
  sender: SenderOfConnection,
  createLifeCycle: ()=>LifeCycle,
  mainEnv: DBEnv,
  createSnapshot: (LifeCycle,RawTx)=>Runnable
) {
  private def runSnapshot() = {
    val lifeCycle = createLifeCycle()
    lifeCycle.open()
    val rawTx = mainEnv.createTx(lifeCycle, rw = false)
    val snapshot = createSnapshot(lifeCycle, rawTx)
    snapshot.run()
  }
  def apply(): Unit = try {
    registrar.register()
    while(true) runSnapshot()
  } catch {
    case e: Exception ⇒
      sender.send("fail", "") //todo
      throw e
  } finally {
    lifeCycle.close()
  }
}

////

trait SessionState {
  def sessionKey: UUID
}

case class NodeAttr[Value](node: DBNode, attr: Attr[Value]) {
  def apply() = node(attr)
  def update(value: Value) = node(attr) = value
}

class IncrementalSnapshot(
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

) extends TxComponent {
  def createSimpleEventSource(sessionId: Long) = new SimpleEventSource(sessionId,
    Single.option(mainSessionsBySessionId.list(sessionId)).getOrElse{
      val mainSession = createObj(isMainSessionAttr)
      mainSession(sessionIdAttr) = Some(sessionId)
      mainSession
    }
  )

  class SimpleEventSource(val byValue: Long, val seqNode: DBNode) extends EventSource[Long] {
    def seqAttr = unmergedEventsFromIdAttr
    def all = eventsBySessionId
  }
  trait EventSource[Value] {
    def seqNode: DBNode
    def seqAttr: Attr[Option[Long]]
    def all: ListByValue[Value]
    def byValue: Value
    def poll(): Option[DBNode] = {
      val eventsFromId: Long = seqNode(seqAttr).getOrElse(0L)
      val eventOpt = Single.option(all.list(byValue, eventsFromId, 1L))
      if(eventOpt.isEmpty){ return None }
      seqNode(seqAttr) = Some(eventOpt.get.objId+1L)
      if(undoByEvent.list(eventOpt.get).isEmpty) eventOpt else poll()
    }
  }



  // if(untilReqId < ev.objId){ return }
  def sessionIncrementalApply(): Unit = {
    val instantSession = Single(instantSessionsBySessionKey.list(sessionState.sessionKey))
    // or create?
    val src = new SimpleEventSource(instantSession.objId)
    var eventOpt = src.poll()
    while(eventOpt.nonEmpty){
      ??? // apply
      eventOpt = src.poll()
    }
  }
  def mergerIncrementalApply(): Unit = {
    val seqNode = getSeqNode()
    val requestsFromId = seqNode(unmergedRequestsFromIdAttr).getOrElse(0L)
    val requestOpt = Single.option(requestsAll.list(true, requestsFromId, 1L))
    if(requestOpt.isEmpty){ return }
    val rq = requestOpt.get
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