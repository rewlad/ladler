package ee.cone.base.db

import java.util.UUID
import ee.cone.base.connection_api.{SwitchSession, CoHandler, CoHandlerProvider}
import ee.cone.base.db.Types._
import ee.cone.base.util.{Never, Single}

class SessionEventSourceOperationsImpl(
  ops: EventSourceOperations, at: SessionEventSourceAttrs,
  instantTxManager: DefaultTxManager[InstantEnvKey], mainTxManager: SessionMainTxManager,
  nodeFactory: NodeFactory, allNodes: DBNodes
) extends SessionEventSourceOperations with CoHandlerProvider {
  private var sessionKeyOpt: Option[UUID] = None
  private def findSession(): Option[DBNode] = {
    val tx = instantTxManager.currentTx()
    Single.option(allNodes.where(
      tx, at.asInstantSession.defined,
      at.sessionKey, sessionKeyOpt.get
    ))
  }
  private def findOrAddSession() = findSession().getOrElse{
    val tx = instantTxManager.currentTx()
    val instantSession = allNodes.create(tx, at.asInstantSession)
    instantSession(at.sessionKey) = sessionKeyOpt.get
    instantSession
  }
  def incrementalApplyAndView[R](view: () => R) = {
    instantTxManager.roTx { () ⇒
      val instantSession = findSession().get
      mainTxManager.muxTx(needRecreate(instantSession)){ () ⇒
        ops.applyEvents(instantSession, Long.MaxValue)
        view()
      }
    }
  }
  private var lastStatusId: ObjId = 0
  private def needRecreate(instantSession: DBNode): Boolean = {
    val tx = instantTxManager.currentTx()
    val newStatusIds = allNodes.where(
      tx, at.asEventStatus.defined,
      at.instantSession, instantSession,
      Some(lastStatusId+1), Long.MaxValue
    ).map(_.objId)
    if(newStatusIds.isEmpty) return false
    lastStatusId = newStatusIds.max
    true
  }
  def addUndo(eventObjId: ObjId) = instantTxManager.rwTx { () ⇒
    val instantSession = findSession().get
    val tx = instantTxManager.currentTx()
    val event = nodeFactory.toNode(tx, eventObjId)
    if (event(at.instantSession) != instantSession) Never()
    val requests = allNodes.where(
      tx, at.asRequest.defined,
      at.instantSession, instantSession,
      Some(eventObjId), Long.MaxValue
    )
    if (requests.exists(!ops.isUndone(_))) throw new Exception("event is requested")
    ops.addEventStatus(event, ok = false)
  }
  def addEvent(applyAttr: Attr[Boolean])(fill: DBNode=>Unit): Unit = instantTxManager.rwTx { () ⇒
    val instantSession = findSession().get
    val ev = allNodes.create(instantSession.tx, at.asEvent)
    ev(at.instantSession) = instantSession
    ev(at.applyAttr) = applyAttr
    fill(ev)
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
  def handlers = CoHandler(SwitchSession)(handleSessionKey) :: Nil
}
