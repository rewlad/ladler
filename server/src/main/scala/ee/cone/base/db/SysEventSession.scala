package ee.cone.base.db

import java.util.UUID
import ee.cone.base.connection_api._
import ee.cone.base.db.Types._
import ee.cone.base.util.{Never, Single}

class SessionEventSourceOperationsImpl(
  ops: EventSourceOperations, at: SessionEventSourceAttrs,
  instantTxManager: DefaultTxManager[InstantEnvKey], mainTxManager: SessionMainTxManager,
  findNodes: FindNodes, uniqueNodes: UniqueNodes
) extends SessionEventSourceOperations with CoHandlerProvider {
  private var sessionKeyOpt: Option[UUID] = None
  private def findSession(): Option[Obj] = {
    val tx = instantTxManager.currentTx()
    Single.option(findNodes.where(
      tx, at.asInstantSession.defined,
      at.sessionKey, sessionKeyOpt,
      Nil
    ))
  }
  private def findOrAddSession() = findSession().getOrElse{
    val tx = instantTxManager.currentTx()
    val instantSession = ops.addInstant(uniqueNodes.noNode, at.asInstantSession)
    instantSession(at.sessionKey) = sessionKeyOpt
    instantSession(at.mainSessionSrcId) = Option(UUID.randomUUID)
    instantSession
  }
  def incrementalApplyAndView[R](view: () => R) = {
    instantTxManager.roTx { () ⇒
      val instantSession = findSession().get
      mainTxManager.muxTx(needRecreate(instantSession)){ () ⇒
        ops.applyEvents(instantSession, Nil)
        view()
      }
    }
  }
  private var lastStatusSrcId: Option[UUID] = None
  private def needRecreate(instantSession: Obj): Boolean = {
    val tx = instantTxManager.currentTx()
    val findAfter =
      lastStatusSrcId.map(uuid=>FindAfter(uniqueNodes.whereSrcId(tx, uuid))).toList
    val newStatuses = findNodes.where(
      tx, at.asEventStatus.defined,
      at.instantSession, instantSession,
      FindLastOnly :: findAfter
    )
    if(newStatuses.isEmpty) return false
    val newStatus :: Nil = newStatuses
    lastStatusSrcId = newStatus(uniqueNodes.srcId)
    true
  }
  def addUndo(eventSrcId: UUID) = instantTxManager.rwTx { () ⇒
    val instantSession = findSession().get
    val tx = instantTxManager.currentTx()
    val event = uniqueNodes.whereSrcId(tx, eventSrcId)
    if (event(at.instantSession) != instantSession) Never()
    val requests = findNodes.where(
      tx, at.asRequest.defined,
      at.instantSession, instantSession,
      FindFrom(event) :: Nil
    )
    if (requests.exists(!ops.isUndone(_))) throw new Exception("event is requested")
    ops.addEventStatus(event, ok = false)
  }
  private def addEvent(fill: Obj=>Attr[Boolean]): Unit = instantTxManager.rwTx { () ⇒
    val instantSession = findSession().get
    val ev = ops.addInstant(instantSession, at.asEvent)
    ev(at.applyAttr) = fill(ev)
  }

  def addRequest() = instantTxManager.rwTx { () ⇒
    val instantSession = findSession().get
    val status = ops.addInstant(instantSession, at.asEventStatus)
    status(at.asRequest) = status
    status(at.requested) = ops.requested
  }
  private def handleSessionKey(uuid: UUID): Unit = {
    val uuidOpt = Option(uuid)
    if(sessionKeyOpt == uuidOpt){ return }
    sessionKeyOpt = uuidOpt
    instantTxManager.rwTx{ () ⇒ findOrAddSession() }
  }
  def handlers =
    CoHandler(AddEvent)(addEvent) ::
    CoHandler(SwitchSession)(handleSessionKey) :: Nil
}
