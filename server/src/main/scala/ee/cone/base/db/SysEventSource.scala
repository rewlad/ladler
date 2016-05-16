package ee.cone.base.db

import java.util.UUID

import ee.cone.base.connection_api._
import ee.cone.base.util.{Never, Single}

//! lost calc-s
//! id-ly typing

// no Session?
// apply handling, notify

class EventSourceAttrsImpl(
  attr: AttrFactory,
  label: LabelFactory,
  asDefined: AttrValueType[Boolean],
  asObj: AttrValueType[Obj],
  asAttr: AttrValueType[Attr[Boolean]],
  asUUID: AttrValueType[Option[UUID]],
  asString: AttrValueType[String]
) (
  val mainSeqUUID: UUID = UUID.fromString("270500fe-42f7-4498-ae56-2e836dcda159"),
  val seq: Attr[Obj] = attr("a6479f10-5a99-47d1-a6e9-2c1713b44e3a", asObj),
  val asInstantSession: Attr[Obj] = label("b11bfecb-d53d-4577-870d-d499fbd4d9d3"),
  val sessionKey: Attr[Option[UUID]] = attr("f7d2a81f-ed6b-46c3-b87b-8290f5ef8942", asUUID),
  val asMainSession: Attr[Obj] = label("c4e3189a-956e-4b0c-af03-3af6889ea694"),
  val instantSession: Attr[Obj] = attr("b68c7cb8-f703-4f80-8d40-b4cb818b3619", asObj),
  val lastMergedEvent: Attr[Obj] = attr("50469df3-d7e6-4903-9709-7489cd9f3ecc", asObj),
  val asEvent: Attr[Obj] = label("cf91649f-c7c0-40c7-bfa0-c3165308cfb3"),
  val lastAppliedEvent: Attr[Obj] = attr("771c667e-a78d-46cd-b41d-0e4b16e7721a", asObj),
  val statesAbout: Attr[Obj] = attr("6da732c4-d255-464b-8047-d8515da58d40", asObj),
  val asUndo: Attr[Obj] = label("d46a9cee-6d55-4d3c-aceb-6af11b8a9c0e"),
  val asCommit: Attr[Obj] = label("091ffe85-2317-47e1-91da-59bcd221a480"),
  val lastMergedRequest: Attr[Obj] = attr("9b1e43fc-a60d-4f41-96bc-6504eb0ccb80", asObj),
  val requested: Attr[Boolean] = attr("55b09b31-3af4-402e-963b-522f71646e9e", asDefined),
  //0x001C
  val applyAttr: Attr[Attr[Boolean]] = attr("a105c5e0-aaee-41ca-8f8a-5d4328594670", asAttr),
  val mainSession: Attr[Obj] = attr("363bb985-aa39-48bf-a866-e74dd3584056", asObj),
  val comment: Attr[String] = attr("c0e6114b-bfb2-49fc-b9ef-5110ed3a9521", asString)
) extends SessionEventSourceAttrs

class EventSourceOperationsImpl(
  at: EventSourceAttrsImpl,
  nodeAttrs: NodeAttrs,
  sysAttrs: FindAttrs,
  factIndex: FactIndex, //u
  nodeHandlerLists: CoHandlerLists, //u
  findNodes: FindNodes,
  instantTx: CurrentTx[InstantEnvKey], //u
  mainTx: CurrentTx[MainEnvKey], //u
  searchIndex: SearchIndex,
  mandatory: Mandatory
) extends ForMergerEventSourceOperations with ForSessionEventSourceOperations with CoHandlerProvider {
  import nodeAttrs.nonEmpty
  import at._
  private def isUndone(event: Obj) =
    findNodes.where(instantTx(), at.asUndo, at.statesAbout, event, Nil).nonEmpty
  private def lastInstant = findNodes.zeroNode(at.seq)
  def unmergedEvents(instantSession: Obj): List[Obj] =
    unmergedEvents(instantSession,_(at.lastMergedEvent),lastInstant).list
  class UnmergedEvents(val list: List[Obj])(val needMainSession: ()=>Obj)
  private def unmergedEvents(instantSession: Obj, lastMergedEventOfSession: Obj⇒Obj, upTo: Obj): UnmergedEvents = {
    val mainSession = instantSession(at.mainSession)
    val lastMergedEvent = lastMergedEventOfSession(mainSession)
    val findAfter =
      if(lastMergedEvent(nonEmpty)) FindAfter(lastMergedEvent) :: Nil else Nil
    if(!upTo(nonEmpty)) Never()
    val events = findNodes.where(
      instantTx(), at.asEvent, at.instantSession, instantSession,
      FindUpTo(upTo) :: findAfter
    ).filterNot(isUndone)
    new UnmergedEvents(events)({ () =>
      mainSession(at.asMainSession) = mainSession
      mainSession
    })
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
    val events = unmergedEvents(instantSession, _(at.lastMergedEvent), upTo)
    val mainSession = applyEvents(events)
    mainSession(at.lastMergedEvent) = upTo
  }
  def applyEvents(instantSession: Obj) = {
    val upTo = lastInstant
    val events = unmergedEvents(instantSession, { session ⇒
      val res = session(at.lastAppliedEvent)
      if(res(nonEmpty)) res else session(at.lastMergedEvent)
    }, upTo)
    val mainSession = applyEvents(events)
    mainSession(at.lastAppliedEvent) = upTo
  }
  private def applyEvents(events: UnmergedEvents) = {
    events.list.foreach{ event =>
      // println(s"$markAttr applied: ${event(uniqueNodes.srcId)}")
      factIndex.switchReason(event)
      nodeHandlerLists.single(ApplyEvent(event(at.applyAttr)), ()⇒Never())(event)
      factIndex.switchReason(findNodes.noNode)
    }
    events.needMainSession()
  }

  def nextRequest(): Obj = {
    val mainSeq = findNodes.whereObjId(findNodes.toObjId(mainSeqUUID))
    val lastNode = mainSeq(at.lastMergedRequest)
    val from = if(lastNode(nonEmpty)) FindAfter(lastNode) :: Nil else Nil
    val result = findNodes.where(
      instantTx(), at.asEvent, at.applyAttr, at.requested, FindFirstOnly :: from
    )
    if(result.isEmpty){ return findNodes.noNode }
    val event :: Nil = result
    mainSeq(at.lastMergedRequest) = event
    if(isUndone(event)) nextRequest() else event
  }
  def addInstant(instantSession: Obj, label: Attr[Obj]): Obj = {
    val sNode = findNodes.zeroNode
    val lastObj = sNode(at.seq)
    val res = findNodes.nextNode(if(lastObj(nodeAttrs.nonEmpty)) lastObj else sNode)
    sNode(at.seq) = res
    res(label) = res
    res(at.instantSession) = instantSession
    res
  }

  def handlers: List[BaseCoHandler] =
    List(
      seq,
      asInstantSession,sessionKey,asMainSession,instantSession,lastMergedEvent,
      asEvent,lastAppliedEvent,statesAbout,asUndo,asCommit,lastMergedRequest,
      applyAttr,mainSession,comment
    ).flatMap(factIndex.handlers(_)) :::
      mandatory(asInstantSession,sessionKey,mutual = true) :::
      mandatory(asInstantSession,mainSession,mutual = true) :::
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
}
