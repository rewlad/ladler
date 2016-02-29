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

/*
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
*/

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
  seqAttr: Attr[Option[DBNode]]
) {
  def inc(): DBNode = {
    val seqNode = new DBNodeImpl(0L)(???)
    val res = new DBNodeImpl(seqNode(seqAttr).getOrElse(seqNode).objId + 1L)(???)
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
  lazy val run = new SnapshotRunningConnection(
    registrar, lifeCycle, sender, mainDB
  )
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
    if(!rw) new RawTx(txLifeCycle, rw, createRawIndex(), () => ())
    else {
      txLifeCycle.onClose(()=>lock.unlock())
      lock.lock()
      val index = createRawIndex()
      def commit() = {
        if(!lock.isHeldByCurrentThread) Never()
        synchronized {
          data = index.data
        }
      }
      new RawTx(txLifeCycle, rw, index, commit)
    }
  def start() = ()
}

class TxStarter(
  connectionLifeCycle: LifeCycle, env: DBEnv, searchIndex: SearchIndex,
  checkAll: PreCommitCheckAllOfConnection
) {
  var tx: Option[RawTx] = None
  def needTx(rw: Boolean): Unit = {
    if(tx.isEmpty) {
      val lifeCycle = connectionLifeCycle.sub()
      val rawTx = lifeCycle.of(()=>env.createTx(lifeCycle, rw = rw)).updates(tx=_).value
      lifeCycle.of(()=>rawTx.rawIndex).updates(searchIndex.switchRawIndex)
      lifeCycle.of(()=>()).updates(checkAll.switchTx)
    }
    if(tx.get.rw != rw) Never()
  }
  def commit() = {
    if(!tx.get.rw) Never()
    val fails = checkAll.checkTx()
    if(fails.nonEmpty) throw new Exception(s"$fails")
    closeTx()
  }
  def closeTx() = tx.foreach(_.lifeCycle.close())
}

class SnapshotRunningConnection(
  registrar: Registrar[ConnectionComponent],
  connectionLifeCycle: LifeCycle,
  sender: SenderOfConnection,
  mainTxStarter: TxStarter,
  instantTxStarter: TxStarter,
  eventSourceOperations: EventSourceOperations,
  incoming: BlockingQueue[DictMessage],
  framePeriod: Long
) {
  var vDomData: Option[()] = None
  def apply(): Unit = try {
    registrar.register()
    while(true) {
      mainTxStarter.needTx(rw=false)
      if(vDomData.isEmpty){
        val lifeCycle = instantTxStarter.needTx(rw=false)
        eventSourceOperations.sessionIncrementalApply()
        makeVDom() // send
        lifeCycle.close()
      }
      val message = Option(incoming.poll(framePeriod,TimeUnit.MILLISECONDS))
      dispatch(message.getOrElse(PeriodicMessage)) //dispatch // can close / set refresh time
      // may be close old mainTx/vDom here
    }
  } catch {
    case e: Exception ⇒
      sender.send("fail", "") //todo
      throw e
  } finally {
    connectionLifeCycle.close()
  }
}

////

trait SessionState {
  def sessionKey: UUID
}
trait ListByValueFactory {
  def apply[Value](label: Attr[Boolean], prop: Attr[Option[Value]]): ListByValue[Value]
  def apply(label: Attr[Boolean]): ListByValue[Boolean]
}


class EventSourceAttrs(
  attr: AttrFactory,
  list: ListByValueFactory,
  booleanValueConverter: RawValueConverter[Boolean],
  uuidValueConverter: RawValueConverter[Option[UUID]],
  longValueConverter: RawValueConverter[Option[Long]],
  nodeValueConverter: RawValueConverter[Option[DBNode]]
)(
  val isInstantSession: Attr[Boolean] = attr(0x0010, 0, booleanValueConverter),
    val sessionKey: Attr[Option[UUID]] = attr(0, 0x0011, uuidValueConverter),
  val isMainSession: Attr[Boolean] = attr(0x0012, 0, booleanValueConverter),
    val sessionId: Attr[Option[Long]] = attr(0, 0x0013, longValueConverter),
    val unmergedEventsFromId: Attr[Option[Long]] = attr(0, 0x0014, longValueConverter),
  val isEvent: Attr[Boolean] = attr(0x0015, 0, booleanValueConverter),
  val isUndo: Attr[Boolean] = attr(0x0016, 0, booleanValueConverter),
    val event: Attr[Option[DBNode]] = attr(0, 0x0017, nodeValueConverter),
  val unmergedRequestsFromId: Attr[Option[Long]] = attr(0, 0x0018, longValueConverter),
  val isRequest: Attr[Boolean] = attr(0x0019, 0, booleanValueConverter),
  val isCommit: Attr[Boolean] = attr(0x001A, 0, booleanValueConverter)
)(
  val instantSessionsBySessionKey: ListByValue[UUID] = list(isInstantSession, sessionKey),
  val mainSessionsBySessionId: ListByValue[Long] = list(isMainSession, sessionId),
  val eventsBySessionId: ListByValue[Long] = list(isEvent, sessionId),
  val undoByEvent: ListByValue[DBNode] = list(isUndo, event),
  val requestsAll: ListByValue[Boolean] = list(isRequest)
)
//! lost calc-s
//! id-ly typing

// no SessionState? no instantSession? create
// create undo
// apply handling, notify
class EventSourceOperations(
  sessionState: SessionState,
  mainCreateNode: Attr[Boolean]=>DBNode,
  factIndex: FactIndex,
  mainTxStarter: TxStarter,
  instantTxStarter: TxStarter,
  instantObjIdSequence: ObjIdSequence,
  instantCreateNode: Attr[Boolean]=>DBNode,
  nodeHandlerLists: NodeHandlerLists,
  attrs: ListByDBNode,

  isInstantSessionAttr: Attr[Boolean],
    sessionKeyAttr: Attr[Option[UUID]],
    instantSessionsBySessionKey: ListByValue[UUID],

  isMainSessionAttr: Attr[Boolean],
    sessionIdAttr: Attr[Option[Long]],
    mainSessionsBySessionId: ListByValue[Long],
    unmergedEventsFromIdAttr: Attr[Option[Long]],

  isEvent: Attr[Boolean],
    //sessionIdAttr
    eventsBySessionId: ListByValue[Long], // instant

  isUndo: Attr[Boolean],
    eventAttr: Attr[Option[DBNode]],
    undoByEvent: ListByValue[DBNode],

  mainSeqNode: ()=>DBNode,
    unmergedRequestsFromIdAttr: Attr[Option[Long]],

  isRequest: Attr[Boolean],
    requestsAll: ListByValue[Boolean],

  isCommit: Attr[Boolean]
    // eventIdAttr

) {
  private class EventSource[Value](listByValue: ListByValue[Value], value: Value, seqRef: Ref[Option[Long]]) {
    def poll(): Option[DBNode] = {
      val eventsFromId: Long = seqRef().getOrElse(0L)
      val eventOpt = Single.option(listByValue.list(value, eventsFromId, 1L))
      if(eventOpt.isEmpty){ return None }
      seqRef() = Some(eventOpt.get.objId+1L)
      if(undoByEvent.list(eventOpt.get).isEmpty) eventOpt else poll()
    }
  }
  private def applyEvents(sessionId: Long, isNotLast: DBNode=>Boolean) = {
    val seqNode = Single.option(mainSessionsBySessionId.list(sessionId)).getOrElse{
      val mainSession = mainCreateNode(isMainSessionAttr)
      mainSession(sessionIdAttr) = Some(sessionId)
      mainSession
    }
    val seqRef = seqNode(unmergedEventsFromIdAttr.ref)
    val src = new EventSource(eventsBySessionId, sessionId, seqRef)
    applyEvents(src, isNotLast)
  }
  private def applyEvents(src: EventSource[Long], isNotLast: DBNode=>Boolean): Unit = {
    val eventOpt = src.poll()
    if(eventOpt.isEmpty) { return }
    factIndex.switchSrcObjId(eventOpt.get.objId)
    for(attr <- attrs.list(eventOpt.get))
      nodeHandlerLists.list(ApplyEvent(attr.nonEmpty)) // apply
    factIndex.switchSrcObjId(0L)
    if(isNotLast(eventOpt.get)) applyEvents(src, isNotLast)
  }
  private def findSessionId() = {
    val instantSession = Single(instantSessionsBySessionKey.list(sessionState.sessionKey))
    // or create?
    instantSession.objId
  }
  def sessionIncrementalApplyAndView[R](view: ()=>R): R = {
    instantTxStarter.needTx(rw=false)
    applyEvents(findSessionId(), (_:DBNode)=>false)
    val res = view()
    instantTxStarter.closeTx()
    res
  }
  def mergerIncrementalApply(): Unit = {
    mainTxStarter.needTx(rw=true)
    val seqRef = mainSeqNode()(unmergedRequestsFromIdAttr.ref)
    val reqSrc = new EventSource(requestsAll, true, seqRef)
    val reqOpt = reqSrc.poll()
    if(reqOpt.isEmpty) { return }
    val req = reqOpt.get
    var ok = false
    try {
      val sessionId = req(sessionIdAttr).get
      applyEvents(sessionId, (ev:DBNode)=>
        if(ev.objId<req.objId) true else if(ev.objId==req.objId) false else Never()
      )
      mainTxStarter.commit()
      ok = true
    } finally {
      addEventStatus(req, ok)
    }
    ??? //? notify
  }
  def addEvent(label: Attr[Boolean])(fill: DBNode=>Unit): Unit = addInstant(label){ ev =>
    ev(isEvent) = true
    ev(sessionIdAttr) = Some(findSessionId())
    fill(ev)
  }
  def addRequest() = addEvent(isRequest)(_=>())
  private def addEventStatus(event: DBNode, ok: Boolean): Unit =
    addInstant(if(ok) isCommit else isUndo)(_(eventAttr) = Some(event))
  private def addInstant(label: Attr[Boolean])(fill: DBNode=>Unit): Unit = {
    instantTxStarter.needTx(rw=true)
    fill(instantCreateNode(label))
    instantTxStarter.commit()
  }
}

case class ApplyEvent(attr: Attr[Boolean]) extends NodeEvent[Unit]


/*
components: =>List[ConnectionComponent]

lazy val receivers = components.collect{ case r: ReceiverOfMessage => r }
def dispatch(message: Message) = receivers.foreach(_.receive(message))
*/