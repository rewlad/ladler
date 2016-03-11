package ee.cone.base.db

import java.util.UUID

import ee.cone.base.connection_api._
import ee.cone.base.util.Single

//! lost calc-s
//! id-ly typing

// no Session?
// apply handling, notify

class EventSourceAttrsImpl(
  attr: AttrFactory,
  label: LabelFactory,
  searchIndex: SearchIndex,
  nodeValueConverter: RawValueConverter[Obj],
  attrValueConverter: RawValueConverter[Attr[Boolean]],
  uuidValueConverter: RawValueConverter[Option[UUID]],
  stringValueConverter: RawValueConverter[String],
  mandatory: Mandatory
) (
  val asInstantSession: Attr[Obj] = label(0x0010),
  val sessionKey: Attr[Option[UUID]] = attr(new PropId(0x0011), uuidValueConverter),
  val asMainSession: Attr[Obj] = label(0x0012),
  val instantSession: Attr[Obj] = attr(new PropId(0x0013), nodeValueConverter),
  val lastMergedEvent: Attr[Obj] = attr(new PropId(0x0014), nodeValueConverter),
  val asEvent: Attr[Obj] = label(0x0015),
  val asEventStatus: Attr[Obj] = label(0x0016),
  val event: Attr[Obj] = attr(new PropId(0x0017), nodeValueConverter),
  val asUndo: Attr[Obj] = label(0x0018),
  val asCommit: Attr[Obj] = label(0x0019),
  val lastMergedRequest: Attr[Obj] = attr(new PropId(0x001A), nodeValueConverter),
  val asRequest: Attr[Obj] = label(0x001B),
  val requested: Attr[String] = attr(new PropId(0x001C), stringValueConverter),
  val applyAttr: Attr[Attr[Boolean]] = attr(new PropId(0x001D), attrValueConverter)
)(val handlers: List[BaseCoHandler] =
  mandatory(asInstantSession,sessionKey,mutual = true) :::
    mandatory(asMainSession,instantSession,mutual = true) :::
    mandatory(asMainSession,lastMergedEvent,mutual = true) :::
    mandatory(asEvent,instantSession,mutual = true) :::
    mandatory(asEvent,applyAttr,mutual = true) :::
    mandatory(asEventStatus,instantSession,mutual = true) :::
    mandatory(asRequest,requested,mutual = true) :::
    mandatory(event,asEventStatus,mutual = false) :::
    mandatory(asRequest,asEventStatus,mutual = false) :::
    mandatory(asUndo, asEventStatus, mutual = false) :::
    mandatory(asCommit, asEventStatus, mutual = false) :::
    searchIndex.handlers(asInstantSession, sessionKey) ::: ////
    searchIndex.handlers(asMainSession, instantSession) ::: //
    searchIndex.handlers(asEvent, instantSession) ::: //
    searchIndex.handlers(asUndo, event) ::: //
    searchIndex.handlers(asRequest, requested) ::: ///
    searchIndex.handlers(asEventStatus, instantSession) ::: ////
    searchIndex.handlers(asRequest, instantSession) ::: ////
    Nil
) extends CoHandlerProvider with MergerEventSourceAttrs with SessionEventSourceAttrs

class EventSourceOperationsImpl(
  at: EventSourceAttrsImpl,
  factIndex: FactIndex, //u
  nodeHandlerLists: CoHandlerLists, //u
  allNodes: DBNodes,
  instantTx: CurrentTx[InstantEnvKey], //u
  mainTx: CurrentTx[MainEnvKey] //u
) extends EventSourceOperations {
  def isUndone(event: Obj) =
    allNodes.where(instantTx(), at.asUndo.defined, at.event, event, Nil).nonEmpty
  def createEventSource[Value](
      label: Attr[Obj], prop: Attr[Value], value: Value,
      seqRef: Ref[Obj], options: List[SearchOption]
  ) =  new EventSource {
    def poll(): Obj = {
      val lastNode = seqRef()
      val from = if(lastNode.nonEmpty) FindAfter(lastNode) :: Nil else Nil
      val result = allNodes.where(instantTx(), label.defined, prop, value, FindFirstOnly :: from ::: options)
      if(result.isEmpty){ return allNodes.noNode }
      val event :: Nil = result
      seqRef() = event
      if(isUndone(event)) poll() else event
    }
  }
  def ref(node: Obj, attr: Attr[Obj]) = new Ref[Obj] {
    def apply() = node(attr)
    def update(value: Obj) = node(attr) = value
  }
  def applyEvents(instantSessionNode: Obj, options: List[SearchOption]): Unit = {
    val sessions = allNodes.where(mainTx(), at.asMainSession.defined, at.instantSession, instantSessionNode, Nil)
    val seqNode = Single.option(sessions).getOrElse{

      UUID.nameUUIDFromBytes()

      val mainSession = allNodes.create(mainTx(), at.asMainSession)
      mainSession(at.instantSession) = instantSessionNode
      mainSession
    }
    val seqRef = ref(seqNode, at.lastMergedEvent)
    val src = createEventSource(at.asEvent, at.instantSession, instantSessionNode, seqRef, options)
    var event = src.poll()
    while(event.nonEmpty){
      factIndex.switchReason(event)
      nodeHandlerLists.single(ApplyEvent(event(at.applyAttr)))(event)
      factIndex.switchReason(allNodes.noNode)
      event = src.poll()
    }
  }
  def addEventStatus(event: Obj, ok: Boolean) = {
    val status = addInstant(event(at.instantSession), at.asEventStatus)
    status(if(ok) at.asCommit else at.asUndo) = status
    status(at.event) = event
  }
  def addInstant(instantSession: Obj, label: Attr[Obj]): Obj = {
    val res = allNodes.create(instantTx(), label, UUID.randomUUID)
    res(at.instantSession) = instantSession
    res
  }
  def requested = "Y"
}
