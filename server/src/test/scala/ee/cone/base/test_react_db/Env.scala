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

class IncrementalSnapshot(
  sessionState: SessionState,
  //searchCommittedSessionId: ListByValue[UUID],
  //seqNode: DBNode,
  instantSessionsBySessionKey: ListByValue[UUID],
  mainSessionsBySessionId: ListByValue[Long],
  instantSessionIdAttr: Attr[Long],
  lastMergedRequestIdAttr: Attr[Long],
  undoEventIdAttr: Attr[Long],
  //reqIdAttr: Attr[Long]
  undoBySessionId: ListByValue[Long],
  eventsBySessionId: ListByValue[Long] // instant
) extends TxComponent {
  def init(): Unit = {
    val instantSession = Single(instantSessionsBySessionKey.list(sessionState.sessionKey))
      // or create?
    val sessionId = instantSession.objId
    val mainSession = Single.option(mainSessionsBySessionId.list(sessionId))
      // or create?
    val eventsFromId: Long = mainSession.map(_(lastMergedRequestIdAttr)+1).getOrElse(0)
      // set by merger; set later



    val undone = undoBySessionId.list(sessionId, eventsFromId).map(_(undoEventIdAttr)).toSet
    val events = eventsBySessionId.list(sessionId, eventsFromId).filter(node => !undone(node.objId))

    val lastMergedReqId = seqNode(lastMergedRequestIdAttr)
    val commits: List[DBNode] =
      searchCommittedSessionId.list(sessionState.sessionKey).sortBy(-_.objId)

    def f(commits: List[DBNode]): List[DBNode] = {
      val commit :: more = commits
      val reqId = commit(reqIdAttr)
      if(reqId <= lastMergedReqId)
    }

    ???
  }
  def incrementalApply(): Unit = {
    ???
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