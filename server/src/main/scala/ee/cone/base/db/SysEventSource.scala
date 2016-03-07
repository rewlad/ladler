package ee.cone.base.db

import java.util.UUID

import ee.cone.base.connection_api.{CoHandlerProvider, CoHandlerLists,
BaseCoHandler}
import ee.cone.base.util.Single

//! lost calc-s
//! id-ly typing

// no Session?
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
    mandatory(asEvent,instantSession,mutual = true) :::
    mandatory(asEventStatus,instantSession,mutual = true) :::
    mandatory(asRequest,requested,mutual = true) :::
    mandatory(event,asEventStatus,mutual = false) :::
    mandatory(asRequest,asEventStatus,mutual = false) :::
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
  factIndex: FactIndex, //u
  nodeHandlerLists: CoHandlerLists, //u
  attrs: ListByDBNode, //u
  allNodes: DBNodes,
  nodeFactory: NodeFactory, //u
  instantTx: CurrentTx[InstantEnvKey], //u
  mainTx: CurrentTx[MainEnvKey] //u
) extends EventSourceOperations {
  def isUndone(event: DBNode) =
    allNodes.where(instantTx(), at.asUndo.defined, at.event, event).nonEmpty
  def createEventSource[Value](label: Attr[DBNode], prop: Attr[Value], value: Value, seqRef: Ref[DBNode], cond: DBNode=>Boolean) =
    new EventSource {
      def poll(): DBNode = {
        val lastNode = seqRef()
        val fromObjId = if(lastNode.nonEmpty) Some(lastNode.objId+1) else None
        val result = allNodes.where(instantTx(), label.defined, prop, value, fromObjId, 1L)
        if(result.isEmpty){ return nodeFactory.noNode }
        val event :: Nil = result
        if(!cond(event)){ return nodeFactory.noNode }
        seqRef() = event
        if(isUndone(event)) poll() else event
      }
    }
  def applyEvents(instantSessionNode: DBNode, cond: DBNode=>Boolean): Unit = {
    val sessions = allNodes.where(mainTx(), at.asMainSession.defined, at.instantSession, instantSessionNode)
    val seqNode = Single.option(sessions).getOrElse{
      val mainSession = allNodes.create(mainTx(), at.asMainSession)
      mainSession(at.instantSession) = instantSessionNode
      mainSession
    }
    val seqRef = seqNode(at.lastMergedEvent.ref)
    val src = createEventSource(at.asEvent, at.instantSession, instantSessionNode, seqRef, cond)
    var event = src.poll()
    while(event.nonEmpty){
      factIndex.switchSrcObjId(event.objId)
      for(attr <- attrs.list(event))
        nodeHandlerLists.list(ApplyEvent(attr.defined)) // apply
      factIndex.switchSrcObjId(0L)
      event = src.poll()
    }
  }
  def addEventStatus(event: DBNode, ok: Boolean) = {
    val status = allNodes.create(event.tx, at.asEventStatus)
    status(if(ok) at.asCommit else at.asUndo) = status
    status(at.event) = event
    status(at.instantSession) = instantSession
  }
  def requested = "Y"
}
