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
  selfValueConverter: RawValueConverter[DBNode],
  uuidValueConverter: RawValueConverter[UUID],
  nodeValueConverter: RawValueConverter[DBNode],
  stringValueConverter: RawValueConverter[String],
  mandatory: Mandatory
) (
  val asInstantSession: Attr[DBNode] = attr(0x0010, 0, selfValueConverter),
  val sessionKey: Attr[UUID] = attr(0, 0x0011, uuidValueConverter),
  val asMainSession: Attr[DBNode] = attr(0x0012, 0, selfValueConverter),
  val instantSession: Attr[DBNode] = attr(0, 0x0013, nodeValueConverter),
  val lastMergedEvent: Attr[DBNode] = attr(0, 0x0014, nodeValueConverter),
  val asEvent: Attr[DBNode] = attr(0x0015, 0, selfValueConverter),
  val asEventStatus: Attr[DBNode] = attr(0x0016, 0, selfValueConverter),
  val event: Attr[DBNode] = attr(0, 0x0017, nodeValueConverter),
  val asUndo: Attr[DBNode] = attr(0x0018, 0, selfValueConverter),
  val asCommit: Attr[DBNode] = attr(0x0019, 0, selfValueConverter),
  val lastMergedRequest: Attr[DBNode] = attr(0, 0x001A, nodeValueConverter),
  val requested: Attr[String] = attr(0x001B, 0, stringValueConverter)
)(val handlers: List[BaseCoHandler] =
  mandatory(asInstantSession,sessionKey,mutual = true) :::
    mandatory(asMainSession,instantSession,mutual = true) :::
    mandatory(asMainSession,lastMergedEvent,mutual = true) :::
    mandatory(asEventStatus,event,mutual = true) :::
    mandatory(asUndo, asEventStatus, mutual = false) :::
    mandatory(asCommit, asEventStatus, mutual = false) :::
    mandatory(requested, asEvent, mutual = false) :::
    searchIndex.handlers(asInstantSession.defined, sessionKey) :::
    searchIndex.handlers(asMainSession, instantSession) :::
    searchIndex.handlers(asEvent, instantSession) :::
    searchIndex.handlers(asUndo, event) :::
    searchIndex.handlers(asEvent, requested) :::
    Nil
) extends CoHandlerProvider with MergerEventSourceAttrs with SessionEventSourceAttrs

/**
  * val handler = Single(handlerLists.list(searchKey))
  * val tx = txManager.tx
  * new ListByValue[Value] {
  * def list(value: Value) = {
  * val feed = new ListFeedImpl[DBNode](Long.MaxValue,(objId,_)=>createNode(objId))
  * val request = new SearchRequest[Value](tx, value, None, feed)
  * handler(request)
  * feed.result.reverse
  * }
  * }
  *
  * */

class EventSourceOperationsImpl(
  at: EventSourceAttrsImpl,
  instantTxManager: TxManager[InstantEnvKey],
  factIndex: FactIndex,
  nodeHandlerLists: CoHandlerLists,
  attrs: ListByDBNode,
  mainValues: ListByValueStart[MainEnvKey],
  instantValues: ListByValueStart[InstantEnvKey],
  mainCreateNode: Attr[DBNode]=>DBNode,
  instantCreateNode: Attr[DBNode]=>DBNode,
  nodeValueConverter: RawValueConverter[DBNode]
) extends EventSourceOperations {
  def createEventSource[Value](prop: Attr[Value], value: Value, seqRef: Ref[DBNode]) =
    new EventSource {
      def poll(): DBNode = {
        val lastNode = seqRef()
        val fromObjId = if(lastNode.nonEmpty) Some(lastNode.objId+1) else None

        val tx = instantTxManager.tx
        val label = at.asEvent
        val searchKey = SearchByLabelProp[Value](label.defined, prop.defined)
        val handler = Single(nodeHandlerLists.list(searchKey))
        val feed = new Feed {
          var result: DBNode = nodeValueConverter.convert()
          def apply(valueA: Long, valueB: Long) = {
            result = nodeValueConverter.convert(0L,valueA)
            false
          }
        }
        val request = new SearchRequest[Value](tx, value, fromObjId, feed)
        handler(request)
        val event = feed.result
        if(!event.nonEmpty){ return event }
        seqRef() = event
        if(instantValues.of(at.asUndo.defined, at.event).list(event).isEmpty) event
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
    val seqRef = seqNode(at.lastMergedEvent.ref)
    val src = createEventSource(at.instantSession, instantSessionNode, seqRef)
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
  def addEventStatus(event: DBNode, ok: Boolean): Unit =
    addInstant(at.asEventStatus){ ev =>
      ev(if(ok) at.asCommit else at.asUndo) = ev
      ev(at.event) = event
    }
  def addInstant(label: Attr[DBNode])(fill: DBNode=>Unit): Unit = {
    instantTxManager.needTx(rw=true)
    fill(instantCreateNode(label))
    instantTxManager.commit()
  }
  def requested = "Y"
}
