package ee.cone.base.db

import java.util.UUID
import ee.cone.base.connection_api.{SwitchSession, CoHandler, BaseCoHandler,
CoHandlerProvider}
import ee.cone.base.db.Types._
import ee.cone.base.util.{Never, Single}

class SessionEventSourceOperationsImpl(
  ops: EventSourceOperations, at: SessionEventSourceAttrs,
  instantTxManager: TxManager[InstantEnvKey], mainTxManager: TxManager[MainEnvKey],
  nodeFactory: NodeFactory, allNodes: DBNodes, instantTx: CurrentTx[InstantEnvKey]
) extends SessionEventSourceOperations with CoHandlerProvider {
  private var sessionKeyOpt: Option[UUID] = None
  private def findSession(): Option[DBNode] = // or create?
    Single.option(allNodes.where(instantTx(), at.asInstantSession.defined, at.sessionKey, sessionKeyOpt.get))
  private def findOrAddSession() = findSession().getOrElse{
    val instantSession = allNodes.create(instantTx(), at.asInstantSession)
    instantSession(at.sessionKey) = sessionKeyOpt.get
    instantSession
  }
  def incrementalApplyAndView[R](view: () => R) = {
    mainTxManager.needTx(rw=false)
    instantTxManager.needTx(rw=false)
    ops.applyEvents(findSession().get, (_:DBNode)=>false)
    val res = view()
    instantTxManager.closeTx()
    res
  }
  def addUndo(eventObjId: ObjId) = {
    instantTxManager.needTx(rw=true)
    val instantSession = findSession().get
    val tx = instantTx()
    val event = nodeFactory.toNode(tx, eventObjId)
    if(event(at.instantSession) != instantSession) Never()
    val requests = allNodes.where(tx, at.asRequest.defined, at.instantSession, instantSession, Some(eventObjId), Long.MaxValue)
    if(requests.exists(!ops.isUndone(_))) throw new Exception("event is requested")
    ops.addEventStatus(event, ok=false)
    instantTxManager.commit()
    mainTxManager.closeTx()
  }
  def addEvent(fill: DBNode=>Unit): Unit = {
    instantTxManager.needTx(rw=true)
    val instantSession = findSession().get
    val ev = allNodes.create(instantSession.tx, at.asEvent)
    ev(at.instantSession) = instantSession
    fill(ev)
    instantTxManager.commit()
  }
  def addRequest() = addEvent { ev =>
    ev(at.asRequest) = ev
    ev(at.requested) = ops.requested
  }
  private def handleSessionKey(uuid: UUID): Unit = {
    val uuidOpt = Option(uuid)
    if(sessionKeyOpt == uuidOpt){ return }
    sessionKeyOpt = uuidOpt
    instantTxManager.needTx(rw=true)
    findOrAddSession()
    instantTxManager.commit()
  }
  def handlers = CoHandler(SwitchSession :: Nil)(handleSessionKey) :: Nil
}
