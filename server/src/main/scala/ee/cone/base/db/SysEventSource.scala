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
  val asInstantSession: Attr[DBNode] = attr(0x0010, 0, selfValueConverter),
  val sessionKey: Attr[Option[UUID]] = attr(0, 0x0011, uuidValueConverter),
  val asMainSession: Attr[DBNode] = attr(0x0012, 0, selfValueConverter),
  val instantSession: Attr[DBNode] = attr(0, 0x0013, longValueConverter),
  val unmergedEventsFromId: Attr[DBNode] = attr(0, 0x0014, longValueConverter),
  val asEvent: Attr[DBNode] = attr(0x0015, 0, selfValueConverter),
  val asEventStatus: Attr[DBNode] = attr(0x0016, 0, selfValueConverter),
  val event: Attr[DBNode] = attr(0, 0x0017, nodeValueConverter),
  val asUndo: Attr[DBNode] = attr(0x0018, 0, selfValueConverter),
  val asCommit: Attr[DBNode] = attr(0x0019, 0, selfValueConverter),
  val unmergedRequestsFrom: Attr[DBNode] = attr(0, 0x001A, longValueConverter),
  val asRequest: Attr[DBNode] = attr(0x001B, 0, selfValueConverter)
)(val handlers: List[BaseCoHandler] =
  mandatory(asInstantSession,sessionKey,mutual = true) :::
    mandatory(asMainSession,instantSession,mutual = true) :::
    mandatory(asMainSession,unmergedEventsFromId,mutual = true) :::
    mandatory(asEventStatus,event,mutual = true) :::
    mandatory(asUndo, asEventStatus, mutual = false) :::
    mandatory(asCommit, asEventStatus, mutual = false) :::
    mandatory(asRequest, asEvent, mutual = false) :::
    searchIndex.handlers(asInstantSession.defined, sessionKey) :::
    searchIndex.handlers(asMainSession, instantSession) :::
    searchIndex.handlers(asEvent, instantSession) :::
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
  mainCreateNode: Attr[DBNode]=>DBNode,
  instantCreateNode: Attr[DBNode]=>DBNode
) extends EventSourceOperations {
  def createEventSource[Value](listByValue: ListByValue[Value], value: Value, seqRef: Ref[DBNode]) =
    new EventSource {
      def poll(): Option[DBNode] = {
        val eventsFromNode = seqRef()

        val eventOpt = Single.option(listByValue.list(value, eventsFromNode, 1L))
        if(eventOpt.isEmpty){ return None }
        seqRef() = instantNode(eventOpt.get.objId+1L)
        if(instantValues.of(at.asUndo.defined, at.event).list(eventOpt).isEmpty) eventOpt
        else poll()
      }
    }
  def applyEvents(instantSessionNode: DBNode, isNotLast: DBNode=>Boolean): Unit = {
    val sessions = mainValues.of(at.asMainSession.defined, at.instantSession).list(instantSessionNode)
    val seqNode = Single.option(sessions).getOrElse{
      val mainSession = mainCreateNode(at.asMainSession)
      mainSession(at.instantSession) = instantSessionNode
      mainSession
    }
    val seqRef = seqNode(at.unmergedEventsFromId.ref)
    val src = createEventSource[DBNode](instantValues.of(at.asEvent.defined, at.instantSession), instantSessionNode, seqRef)
    applyEvents(src, isNotLast)
  }
  private def applyEvents(src: EventSource, isNotLast: DBNode=>Boolean): Unit = {
    val eventOpt = src.poll()
    if(eventOpt.isEmpty) { return }
    factIndex.switchSrcObjId(eventOpt.get.objId)
    for(attr <- attrs.list(eventOpt.get))
      nodeHandlerLists.list(ApplyEvent(attr.defined)) // apply
    factIndex.switchSrcObjId(0L)
    if(isNotLast(eventOpt.get)) applyEvents(src, isNotLast)
  }
  def addEventStatus(event: DBNode, ok: Boolean): Unit =
    addInstant(at.asEventStatus){ ev =>
      ev(if(ok) at.asCommit else at.asUndo) = Some(ev)
      ev(at.event) = Some(event)
    }
  def addInstant(label: Attr[DBNode])(fill: DBNode=>Unit): Unit = {
    instantTxStarter.needTx(rw=true)
    fill(instantCreateNode(label))
    instantTxStarter.commit()
  }
}
