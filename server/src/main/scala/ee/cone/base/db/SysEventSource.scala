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
)(val handlers: List[BaseCoHandler] =
  mandatory(asInstantSession,sessionKey,mutual = true) :::
    mandatory(asMainSession,sessionId,mutual = true) :::
    mandatory(asMainSession,unmergedEventsFromId,mutual = true) :::
    mandatory(asEventStatus,event,mutual = true) :::
    mandatory(asUndo, asEventStatus, mutual = false) :::
    mandatory(asCommit, asEventStatus, mutual = false) :::
    mandatory(asRequest, asEvent, mutual = false) :::
    searchIndex.handlers(asInstantSession.nonEmpty, sessionKey) :::
    searchIndex.handlers(asMainSession, sessionId) :::
    searchIndex.handlers(asEvent, sessionId) :::
    searchIndex.handlers(asUndo, event) :::
    searchIndex.handlers(asRequest) :::
    Nil
) extends CoHandlerProvider with MergerEventSourceAttrs with SessionEventSourceAttrs

class EventSourceOperationsImpl(
  at: EventSourceAttrsImpl,
  instantTxStarter: TxManager[InstantEnvKey],
  factIndex: FactIndex,
  nodeHandlerLists: CoHandlerLists,
  attrs: ListByDBNode,
  mainValues: ListByValueStart[MainEnvKey],
  instantValues: ListByValueStart[InstantEnvKey],
  mainCreateNode: Attr[Option[DBNode]]=>DBNode,
  instantCreateNode: Attr[Option[DBNode]]=>DBNode
) extends EventSourceOperations {
  def createEventSource[Value](listByValue: ListByValue[Value], value: Value, seqRef: Ref[Option[Long]]) =
    new EventSource {
      def poll(): Option[DBNode] = {
        val eventsFromId: Long = seqRef().getOrElse(0L)
        val eventOpt = Single.option(listByValue.list(value, Some(eventsFromId), 1L))
        if(eventOpt.isEmpty){ return None }
        seqRef() = Some(eventOpt.get.objId+1L)
        if(instantValues.of(at.asUndo.nonEmpty, at.event).list(eventOpt).isEmpty) eventOpt
        else poll()
      }
    }
  def applyEvents(sessionId: Long, isNotLast: DBNode=>Boolean): Unit = {
    val sessions = mainValues.of(at.asMainSession.nonEmpty, at.sessionId).list(Some(sessionId))
    val seqNode = Single.option(sessions).getOrElse{
      val mainSession = mainCreateNode(at.asMainSession)
      mainSession(at.sessionId) = Some(sessionId)
      mainSession
    }
    val seqRef = seqNode(at.unmergedEventsFromId.ref)
    val src = createEventSource[Option[Long]](instantValues.of(at.asEvent.nonEmpty, at.sessionId), Some(sessionId), seqRef)
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
