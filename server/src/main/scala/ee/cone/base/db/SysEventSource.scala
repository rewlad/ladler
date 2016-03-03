package ee.cone.base.db

import java.util.UUID

import ee.cone.base.connection_api.{CoHandlerProvider, CoHandlerLists,
BaseCoHandler}
import ee.cone.base.util.Single

//! lost calc-s
//! id-ly typing

// no SessionState? no instantSession? create
// create undo
// apply handling, notify

class EventSourceAttrsImpl(
  attr: AttrFactory,
  searchIndex: SearchIndex,
  nodeValueConverter: RawValueConverter[DBNode],
  uuidValueConverter: RawValueConverter[UUID],
  stringValueConverter: RawValueConverter[String],
  mandatory: Mandatory
) (
  val asInstantSession: Attr[DBNode] = attr(0x0010, 0, nodeValueConverter),
  val sessionKey: Attr[UUID] = attr(0, 0x0011, uuidValueConverter),
  val asMainSession: Attr[DBNode] = attr(0x0012, 0, nodeValueConverter),
  val instantSession: Attr[DBNode] = attr(0, 0x0013, nodeValueConverter),
  val lastMergedEvent: Attr[DBNode] = attr(0, 0x0014, nodeValueConverter),
  val asEvent: Attr[DBNode] = attr(0x0015, 0, nodeValueConverter),
  val asEventStatus: Attr[DBNode] = attr(0x0016, 0, nodeValueConverter),
  val event: Attr[DBNode] = attr(0, 0x0017, nodeValueConverter),
  val asUndo: Attr[DBNode] = attr(0x0018, 0, nodeValueConverter),
  val asCommit: Attr[DBNode] = attr(0x0019, 0, nodeValueConverter),
  val lastMergedRequest: Attr[DBNode] = attr(0, 0x001A, nodeValueConverter),
  val asRequest: Attr[DBNode] = attr(0x001B, 0, nodeValueConverter),
  val requested: Attr[String] = attr(0, 0x001C, stringValueConverter)
)(val handlers: List[BaseCoHandler] =
  mandatory(asInstantSession,sessionKey,mutual = true) :::
    mandatory(asMainSession,instantSession,mutual = true) :::
    mandatory(asMainSession,lastMergedEvent,mutual = true) :::
    mandatory(asEventStatus,event,mutual = true) :::
    mandatory(asRequest,requested,mutual = true) :::
    mandatory(asRequest,asEvent,mutual = false) :::
    mandatory(asUndo, asEventStatus, mutual = false) :::
    mandatory(asCommit, asEventStatus, mutual = false) :::
    searchIndex.handlers(asInstantSession.defined, sessionKey) :::
    searchIndex.handlers(asMainSession, instantSession) :::
    searchIndex.handlers(asEvent, instantSession) :::
    searchIndex.handlers(asUndo, event) :::
    searchIndex.handlers(asEvent, requested) :::
    searchIndex.handlers(asRequest, instantSession) :::
    Nil
) extends CoHandlerProvider with MergerEventSourceAttrs with SessionEventSourceAttrs

class EventSourceOperationsImpl(
  at: EventSourceAttrsImpl,
  instantTxManager: TxManager[InstantEnvKey],
  factIndex: FactIndex,
  nodeHandlerLists: CoHandlerLists,
  attrs: ListByDBNode,
  mainNodes: DBNodes[MainEnvKey],
  instantNodes: DBNodes[InstantEnvKey],
  nodeFactory: NodeFactory
) extends EventSourceOperations {
  def isUndone(event: DBNode) =
    instantNodes.where(at.asUndo.defined, at.event, event).nonEmpty
  def createEventSource[Value](label: Attr[DBNode], prop: Attr[Value], value: Value, seqRef: Ref[DBNode]) =
    new EventSource {
      def poll(): DBNode = {
        val lastNode = seqRef()
        val fromObjId = if(lastNode.nonEmpty) Some(lastNode.objId+1) else None
        val searchKey = SearchByLabelProp[Value](label.defined, prop.defined)
        val result = instantNodes.where(searchKey, value, fromObjId, 1L)
        if(result.isEmpty){ return nodeFactory.noNode }
        val event :: Nil = result
        seqRef() = event
        if(isUndone(event)) poll() else event
      }
    }
  def addUndo(event: DBNode) = {
    val instantSession = event(at.instantSession) //check
    val searchKey = SearchByLabelProp[DBNode](at.asRequest.defined,at.instantSession.defined)
    val requests = instantNodes.where(searchKey,instantSession,Some(event.objId),Long.MaxValue)
    if(requests.exists(!isUndone(_))) throw new Exception("")
  }

  def applyEvents(instantSessionNode: DBNode, isNotLast: DBNode=>Boolean): Unit = {
    val sessions = mainNodes.where(at.asMainSession.defined, at.instantSession, instantSessionNode)
    val seqNode = Single.option(sessions).getOrElse{
      val mainSession = mainNodes.create(at.asMainSession)
      mainSession(at.instantSession) = instantSessionNode
      mainSession
    }
    val seqRef = seqNode(at.lastMergedEvent.ref)
    val src = createEventSource(at.asEvent, at.instantSession, instantSessionNode, seqRef)
    applyEvents(src, isNotLast)
  }
  private def applyEvents(src: EventSource, isNotLast: DBNode=>Boolean): Unit = {
    val event = src.poll()
    if(!event.nonEmpty) { return }
    factIndex.switchSrcObjId(event.objId)
    for(attr <- attrs.list(event))
      nodeHandlerLists.list(ApplyEvent(attr.defined)) // apply
    factIndex.switchSrcObjId(0L)
    if(isNotLast(event)) applyEvents(src, isNotLast)
  }
  def addEventStatus(event: DBNode, ok: Boolean) = {
    instantTxManager.needTx(rw=true)
    val ev = instantNodes.create(at.asEventStatus)
    ev(if(ok) at.asCommit else at.asUndo) = ev
    ev(at.event) = event
    instantTxManager.commit()
  }
  def addEvent(instantSession: DBNode, fill: DBNode=>Unit): Unit = {
    instantTxManager.needTx(rw=true)
    val ev = instantNodes.create(at.asEvent)
    ev(at.instantSession) = instantSession
    fill(ev)
    instantTxManager.commit()
  }
  def requested = "Y"
}
