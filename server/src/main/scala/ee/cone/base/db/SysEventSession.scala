package ee.cone.base.db

import java.util.UUID
import ee.cone.base.connection_api.{SwitchSession, CoHandler, BaseCoHandler,
CoHandlerProvider}
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
    Single.option(allNodes.where(tx, at.asInstantSession.defined, at.sessionKey, sessionKeyOpt.get))
  }
  private def findOrAddSession() = findSession().getOrElse{
    val tx = instantTxManager.currentTx()
    val instantSession = allNodes.create(tx, at.asInstantSession)
    instantSession(at.sessionKey) = sessionKeyOpt.get
    instantSession
  }
  def incrementalApplyAndView[R](view: () => R) = {
    mainTxManager.muxTx { () ⇒
      instantTxManager.roTx { () ⇒
        ops.applyEvents(findSession().get, (_: DBNode) => false)
        view()
      }
    }
  }
  def addUndo(eventObjId: ObjId) = {
    instantTxManager.rwTx { () ⇒
      val instantSession = findSession().get
      val tx = instantTxManager.currentTx()
      val event = nodeFactory.toNode(tx, eventObjId)
      if (event(at.instantSession) != instantSession) Never()
      val requests = allNodes.where(tx, at.asRequest.defined, at.instantSession, instantSession, Some(eventObjId), Long.MaxValue)
      if (requests.exists(!ops.isUndone(_))) throw new Exception("event is requested")
      ops.addEventStatus(event, ok = false)
    }
    mainTxManager.invalidate()
  }
  def addEvent(fill: DBNode=>Unit): Unit = instantTxManager.rwTx { () ⇒
    val instantSession = findSession().get
    val ev = allNodes.create(instantSession.tx, at.asEvent)
    ev(at.instantSession) = instantSession
    fill(ev)
  }

  def addRequest() = addEvent { ev =>
    ev(at.asRequest) = ev
    ev(at.requested) = ops.requested
  }
  private def handleSessionKey(uuid: UUID): Unit = {
    val uuidOpt = Option(uuid)
    if(sessionKeyOpt == uuidOpt){ return }
    sessionKeyOpt = uuidOpt
    instantTxManager.rwTx{ () ⇒ findOrAddSession() }
  }
  def handlers = CoHandler(SwitchSession)(handleSessionKey) :: Nil
}
