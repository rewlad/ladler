package ee.cone.base.db

import java.util.UUID

import ee.cone.base.connection_api._
import ee.cone.base.util.{Never, Single}

//! lost calc-s
//! id-ly typing

// no Session?
// apply handling, notify

class EventSourceAttrsImpl(
  sysAttrs: SysAttrs,
  attr: AttrFactory,
  label: LabelFactory,
  searchIndex: SearchIndex,
  definedValueConverter: RawValueConverter[Boolean],
  nodeValueConverter: RawValueConverter[Obj],
  attrValueConverter: RawValueConverter[Attr[Boolean]],
  uuidValueConverter: RawValueConverter[Option[UUID]],
  stringValueConverter: RawValueConverter[String],
  mandatory: Mandatory
) (
  val asInstantSession: Attr[Obj] = label(0x0010),
  val sessionKey: Attr[Option[UUID]] = attr("f7d2a81f-ed6b-46c3-b87b-8290f5ef8942", uuidValueConverter),
  val asMainSession: Attr[Obj] = label("c4e3189a-956e-4b0c-af03-3af6889ea694"),
  val instantSession: Attr[Obj] = attr("b68c7cb8-f703-4f80-8d40-b4cb818b3619", nodeValueConverter),
  val lastMergedEvent: Attr[Obj] = attr("50469df3-d7e6-4903-9709-7489cd9f3ecc", nodeValueConverter),
  val asEvent: Attr[Obj] = label("cf91649f-c7c0-40c7-bfa0-c3165308cfb3"),
  val lastAppliedEvent: Attr[Obj] = attr("771c667e-a78d-46cd-b41d-0e4b16e7721a", nodeValueConverter),
  val statesAbout: Attr[Obj] = attr("6da732c4-d255-464b-8047-d8515da58d40", nodeValueConverter),
  val asUndo: Attr[Obj] = label("d46a9cee-6d55-4d3c-aceb-6af11b8a9c0e"),
  val asCommit: Attr[Obj] = label("091ffe85-2317-47e1-91da-59bcd221a480"),
  val lastMergedRequest: Attr[Obj] = attr("9b1e43fc-a60d-4f41-96bc-6504eb0ccb80", nodeValueConverter),
  val requested: Attr[Boolean] = attr("55b09b31-3af4-402e-963b-522f71646e9e", definedValueConverter),
  //0x001C
  val applyAttr: Attr[Attr[Boolean]] = attr("a105c5e0-aaee-41ca-8f8a-5d4328594670", attrValueConverter),
  val mainSessionSrcId: Attr[Option[UUID]] = attr("363bb985-aa39-48bf-a866-e74dd3584056", uuidValueConverter),
  val comment: Attr[String] = attr("c0e6114b-bfb2-49fc-b9ef-5110ed3a9521", stringValueConverter)
)(val handlers: List[BaseCoHandler] =
    mandatory(asInstantSession,sessionKey,mutual = true) :::
    mandatory(asInstantSession,mainSessionSrcId,mutual = true) :::
    //mandatory(asMainSession,instantSession,mutual = false) :::
    mandatory(asMainSession,lastMergedEvent,mutual = true) :::
    mandatory(asEvent,instantSession,mutual = false) :::
    mandatory(asEvent,applyAttr,mutual = true) :::
    mandatory(asCommit,instantSession,mutual = false) :::
    mandatory(asUndo, statesAbout, mutual = false) :::
    mandatory(asCommit, statesAbout, mutual = false) :::
    searchIndex.handlers(asInstantSession, sessionKey) ::: ////
    //searchIndex.handlers(asMainSession, instantSession) ::: //
    searchIndex.handlers(asEvent, instantSession) ::: //
    searchIndex.handlers(asUndo, statesAbout) ::: //
    searchIndex.handlers(asCommit, statesAbout) ::: //
    searchIndex.handlers(asCommit, sysAttrs.justIndexed) ::: //
    searchIndex.handlers(asEvent, applyAttr) ::: ///
    searchIndex.handlers(asCommit, instantSession) ::: ////
    CoHandler(ApplyEvent(requested))(_=>()) ::
    Nil
) extends CoHandlerProvider with SessionEventSourceAttrs

class EventSourceOperationsImpl(
  at: EventSourceAttrsImpl,
  sysAttrs: SysAttrs,
  factIndex: FactIndex, //u
  nodeHandlerLists: CoHandlerLists, //u
  findNodes: FindNodes,
  uniqueNodes: UniqueNodes,
  instantTx: CurrentTx[InstantEnvKey], //u
  mainTx: CurrentTx[MainEnvKey] //u
) extends ForMergerEventSourceOperations with ForSessionEventSourceOperations {
  private def isUndone(event: Obj) =
    findNodes.where(instantTx(), at.asUndo, at.statesAbout, event, Nil).nonEmpty
  private def lastInstant = uniqueNodes.seqNode(instantTx())(sysAttrs.seq)
  def unmergedEvents(instantSession: Obj): List[Obj] =
    unmergedEvents(instantSession,at.lastMergedEvent,lastInstant).list
  class UnmergedEvents(val list: List[Obj])(val needMainSession: ()=>Obj)
  private def unmergedEvents(instantSession: Obj, markAttr: Attr[Obj], upTo: Obj): UnmergedEvents = {
    val mainSrcId = instantSession(at.mainSessionSrcId).get
    val existingMainSession = uniqueNodes.whereSrcId(mainTx(), mainSrcId)
    val lastMergedEvent =
      if(existingMainSession.nonEmpty) existingMainSession(markAttr)
      else uniqueNodes.noNode
    val findAfter =
      if(lastMergedEvent.nonEmpty) FindAfter(lastMergedEvent) :: Nil else Nil
    if(!upTo.nonEmpty) Never()
    val events = findNodes.where(
      instantTx(), at.asEvent, at.instantSession, instantSession,
      FindUpTo(upTo) :: findAfter
    ).filterNot(isUndone)
    new UnmergedEvents(events)(() =>
      if(existingMainSession.nonEmpty) existingMainSession
        else uniqueNodes.create(mainTx(), at.asMainSession, mainSrcId)
    )
  }

  def undo(ev: Obj) = {
    val status = addInstant(ev(at.instantSession), at.asUndo)
    status(at.statesAbout) = ev
  }
  def addCommit(req: Obj) = {
    val status = addInstant(req(at.instantSession), at.asCommit)
    status(sysAttrs.justIndexed) = findNodes.justIndexed
    status(at.statesAbout) = req
  }

  def applyRequestedEvents(req: Obj) = {
    val upTo = req
    val instantSession = req(at.instantSession)
    val events = unmergedEvents(instantSession, at.lastMergedEvent, upTo)
    val mainSession = applyEvents(events)
    mainSession(at.lastMergedEvent) = upTo
  }
  def applyEvents(instantSession: Obj) = {
    val upTo = lastInstant
    val attr = new Attr[Obj] {
      def set(node: Obj, value: Obj) = Never()
      def get(node: Obj) = {
        val res = node(at.lastAppliedEvent)
        if(res.nonEmpty) res else node(at.lastMergedEvent)
      }
    }
    val events = unmergedEvents(instantSession, attr, upTo)
    val mainSession = applyEvents(events)
    mainSession(at.lastAppliedEvent) = upTo
  }
  private def applyEvents(events: UnmergedEvents) = {
    events.list.foreach{ event =>
      // println(s"$markAttr applied: ${event(uniqueNodes.srcId)}")
      factIndex.switchReason(event)
      nodeHandlerLists.single(ApplyEvent(event(at.applyAttr)))(event)
      factIndex.switchReason(uniqueNodes.noNode)
    }
    events.needMainSession()
  }

  def nextRequest(): Obj = {
    val lastNode = uniqueNodes.seqNode(mainTx())(at.lastMergedRequest)
    val from = if(lastNode.nonEmpty) FindAfter(lastNode) :: Nil else Nil
    val result = findNodes.where(
      instantTx(), at.asEvent, at.applyAttr, at.requested, FindFirstOnly :: from
    )
    if(result.isEmpty){ return uniqueNodes.noNode }
    val event :: Nil = result
    uniqueNodes.seqNode(mainTx())(at.lastMergedRequest) = event
    if(isUndone(event)) nextRequest() else event
  }
  def addInstant(instantSession: Obj, label: Attr[Obj]): Obj = {
    val res = uniqueNodes.create(instantTx(), label, UUID.randomUUID)
    res(at.instantSession) = instantSession
    res
  }
}
