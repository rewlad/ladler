package ee.cone.base.db

import java.util.UUID
import ee.cone.base.connection_api._
import ee.cone.base.db.Types._
import ee.cone.base.util.{Never, Single}

class SessionEventSourceOperationsImpl(
  ops: EventSourceOperations, at: SessionEventSourceAttrs,
  instantTxManager: DefaultTxManager[InstantEnvKey], mainTxManager: SessionMainTxManager,
  allNodes: DBNodes
) extends SessionEventSourceOperations with CoHandlerProvider {
  private var sessionKeyOpt: Option[UUID] = None
  private def findSession(): Option[Obj] = {
    val tx = instantTxManager.currentTx()
    Single.option(allNodes.where(
      tx, at.asInstantSession.defined,
      at.sessionKey, sessionKeyOpt,
      Nil
    ))
  }
  private def findOrAddSession() = findSession().getOrElse{
    val tx = instantTxManager.currentTx()
    val instantSession = allNodes.create(tx, at.asInstantSession)
    instantSession(at.sessionKey) = sessionKeyOpt
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
      lastStatusSrcId.map(uuid=>FindAfter(allNodes.whereSrcId(tx, uuid))).toList
    val newStatuses = allNodes.where(
      tx, at.asEventStatus.defined,
      at.instantSession, instantSession,
      FindLastOnly :: findAfter
    )
    if(newStatuses.isEmpty) return false
    val newStatus :: Nil = newStatuses
    lastStatusSrcId = newStatus(allNodes.srcId)
    true
  }
  def addUndo(eventSrcId: UUID) = instantTxManager.rwTx { () ⇒
    val instantSession = findSession().get
    val tx = instantTxManager.currentTx()
    val event = allNodes.whereSrcId(tx, eventSrcId)
    if (event(at.instantSession) != instantSession) Never()
    val requests = allNodes.where(
      tx, at.asRequest.defined,
      at.instantSession, instantSession,
      FindFrom(event) :: Nil
    )
    if (requests.exists(!ops.isUndone(_))) throw new Exception("event is requested")
    ops.addEventStatus(event, ok = false)
  }
  private def addEvent(fill: Obj=>Attr[Boolean]): Unit = instantTxManager.rwTx { () ⇒
    val instantSession = findSession().get
    val ev = allNodes.create(instantSession.tx, at.asEvent)
    ev(at.instantSession) = instantSession
    ev(at.applyAttr) = fill(ev)
  }

  def addRequest() = instantTxManager.rwTx { () ⇒
    val instantSession = findSession().get
    val status = allNodes.create(instantSession.tx, at.asEventStatus)
    status(at.instantSession) = instantSession
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
