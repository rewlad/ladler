package ee.cone.base.db

import java.util.UUID

import ee.cone.base.connection_api.ConnectionComponent
import ee.cone.base.util.Single

//! lost calc-s
//! id-ly typing

// no SessionState? no instantSession? create
// create undo
// apply handling, notify

class EventSourceAttrsImpl(
  attr: AttrFactory,
  list: ListByValueFactory,
  selfValueConverter: RawValueConverter[Option[DBNode]],
  uuidValueConverter: RawValueConverter[Option[UUID]],
  longValueConverter: RawValueConverter[Option[Long]],
  nodeValueConverter: RawValueConverter[Option[DBNode]],
  mandatory: Mandatory
) (
  val asInstantSession: Attr[Option[DBNode]] = attr(0x0010, 0, selfValueConverter),
  val sessionKey: Attr[Option[UUID]] = attr(0, 0x0011, uuidValueConverter),
  val asMainSession: Attr[Option[DBNode]] = attr(0x0012, 0, selfValueConverter),
  val sessionId: Attr[Option[Long]] = attr(0, 0x0013, longValueConverter),
  val unmergedEventsFromId: Attr[Option[Long]] = attr(0, 0x0014, longValueConverter),
  val asEvent: Attr[Option[DBNode]] = attr(0x0015, 0, selfValueConverter),
  val asEventStatus: Attr[Option[DBNode]] = attr(0x0016, 0, selfValueConverter),
  val event: Attr[Option[DBNode]] = attr(0, 0x0017, nodeValueConverter),
  val asUndo: Attr[Option[DBNode]] = attr(0x0018, 0, selfValueConverter),
  val asCommit: Attr[Option[DBNode]] = attr(0x0019, 0, selfValueConverter),
  val unmergedRequestsFromId: Attr[Option[Long]] = attr(0, 0x001A, longValueConverter),
  val asRequest: Attr[Option[DBNode]] = attr(0x001B, 0, selfValueConverter)

)(
  val instantSessionsBySessionKey: ListByValue[UUID] = list(asInstantSession, sessionKey),
  val mainSessionsBySessionId: ListByValue[Long] = list(asMainSession, sessionId),
  val eventsBySessionId: ListByValue[Long] = list(asEvent, sessionId),
  val undoByEvent: ListByValue[DBNode] = list(asUndo, event),
  val requestsAll: ListByValue[Boolean] = list(asRequest)
)(
  val components: List[ConnectionComponent] =
  mandatory.mutual(asInstantSession,sessionKey) :::
    mandatory.mutual(asMainSession,sessionId) :::
    mandatory.mutual(asMainSession,unmergedEventsFromId) :::
    mandatory.mutual(asEventStatus,event) :::
    mandatory(asUndo, asEventStatus) :::
    mandatory(asCommit, asEventStatus) :::
    mandatory(asRequest, asEvent) :::
    instantSessionsBySessionKey.components :::
    mainSessionsBySessionId.components :::
    eventsBySessionId.components :::
    undoByEvent.components :::
    requestsAll.components :::
    Nil
) extends MergerEventSourceAttrs with SessionEventSourceAttrs

class EventSourceOperationsImpl(
  at: EventSourceAttrsImpl,
  instantTxStarter: TxManager,
  factIndex: FactIndex,
  mainCreateNode: Attr[Option[DBNode]]=>DBNode,
  instantCreateNode: Attr[Option[DBNode]]=>DBNode,
  nodeHandlerLists: NodeHandlerLists,
  attrs: ListByDBNode
) extends EventSourceOperations {
  def createEventSource[Value](listByValue: ListByValue[Value], value: Value, seqRef: Ref[Option[Long]]) =
    new EventSource {
      def poll(): Option[DBNode] = {
        val eventsFromId: Long = seqRef().getOrElse(0L)
        val eventOpt = Single.option(listByValue.list(value, eventsFromId, 1L))
        if(eventOpt.isEmpty){ return None }
        seqRef() = Some(eventOpt.get.objId+1L)
        if(at.undoByEvent.list(eventOpt.get).isEmpty) eventOpt else poll()
      }
    }
  def applyEvents(sessionId: Long, isNotLast: DBNode=>Boolean): Unit = {
    val seqNode = Single.option(at.mainSessionsBySessionId.list(sessionId)).getOrElse{
      val mainSession = mainCreateNode(at.asMainSession)
      mainSession(at.sessionId) = Some(sessionId)
      mainSession
    }
    val seqRef = seqNode(at.unmergedEventsFromId.ref)
    val src = createEventSource(at.eventsBySessionId, sessionId, seqRef)
    applyEvents(src, isNotLast)
  }
  private def applyEvents(src: EventSource, isNotLast: DBNode=>Boolean): Unit = {
    val eventOpt = src.poll()
    if(eventOpt.isEmpty) { return }
    factIndex.switchSrcObjId(eventOpt.get.objId)
    for(attr <- attrs.list(eventOpt.get))
      nodeHandlerLists.list(ApplyEvent(attr.nonEmpty)) // apply
    factIndex.switchSrcObjId(0L)
    if(isNotLast(eventOpt.get)) applyEvents(src, isNotLast)
  }
  def addEventStatus(event: DBNode, ok: Boolean): Unit =
    addInstant(at.asEventStatus){ ev =>
      ev(if(ok) at.asCommit else at.asUndo) = Some(ev)
      ev(at.event) = Some(event)
    }
  def addInstant(label: Attr[Option[DBNode]])(fill: DBNode=>Unit): Unit = {
    instantTxStarter.needTx(rw=true)
    fill(instantCreateNode(label))
    instantTxStarter.commit()
  }
}
