package ee.cone.base.db

import java.util.UUID
import ee.cone.base.connection_api._
import ee.cone.base.db.Types._
import ee.cone.base.util.{Never, Single}

class SessionEventSourceOperationsImpl(
  ops: ForSessionEventSourceOperations,
  at: SessionEventSourceAttrs, nodeAttrs: NodeAttrs, sysAttrs: FindAttrs,
  instantTxManager: DefaultTxManager[InstantEnvKey], mainTxManager: SessionMainTxManager,
  findNodes: FindNodes
)(
    var lastStatus: Obj = findNodes.noNode
) extends SessionEventSourceOperations with CoHandlerProvider {
  private var sessionKeyOpt: Option[UUID] = None
  def sessionKey = sessionKeyOpt.get
  private def findSession(): Option[Obj] = {
    val tx = instantTxManager.currentTx()
    Single.option(findNodes.where(
      tx, ops.findInstantSessionBySessionKey, sessionKeyOpt, Nil
    ))
  }
  private def findOrAddSession() = findSession().getOrElse{
    val tx = instantTxManager.currentTx()
    val instantSession = ops.addInstant(findNodes.noNode, at.asInstantSession)
    instantSession(at.sessionKey) = sessionKeyOpt
    instantSession(at.mainSession) = findNodes.whereObjId(findNodes.toObjId(UUID.randomUUID))
    instantSession
  }
  def incrementalApplyAndView[R](view: () => R) = {
    instantTxManager.roTx { () ⇒
      val instantSession = findSession().get
      mainTxManager.muxTx(needRecreate(instantSession)){ () ⇒
        ops.applyEvents(instantSession)
        view()
      }
    }
  }
  def unmergedEvents = ops.unmergedEvents(findSession().get)
  var decoupled: Boolean = false
  private def needRecreate(instantSession: Obj): Boolean = {
    val tx = instantTxManager.currentTx()
    val findAfter = if(lastStatus(sysAttrs.nonEmpty)) List(FindAfter(lastStatus)) else Nil
    val options = FindLastOnly :: findAfter
    val newStatuses = if(decoupled) findNodes.where(
      tx, ops.findCommitByInstantSession, instantSession, options
    ) else findNodes.where(
      tx, ops.findCommit, findNodes.justIndexed, options
    )
    if(newStatuses.isEmpty) return false
    val newStatus :: Nil = newStatuses
    lastStatus = newStatus
    true
  }
  def addUndo(event: Obj) = instantTxManager.rwTx { () ⇒
    val instantSession = findSession().get
    val objId = event(nodeAttrs.objId)
    mainTxManager.muxTx(recreate=true)(()=>
      ops.unmergedEvents(instantSession).reverse.foreach{ ev =>
        if(ev(nodeAttrs.objId) == objId) ops.undo(ev)
        if(ev(at.applyAttr) == at.requested) Never() // check
      }
    )
  }
  def addEvent(fill: Obj=>(ObjId,String)): Unit = instantTxManager.rwTx { () ⇒
    val instantSession = findSession().get
    val ev = ops.addInstant(instantSession, at.asEvent)
    val (handler, comment) = fill(ev)
    ev(at.applyAttr) = handler
    ev(at.comment) = comment
  }

  def addRequest() = addEvent{ req ⇒ (at.requested, "requested") }
  private def handleSessionKey(uuid: UUID): Unit = {
    val uuidOpt = Option(uuid)
    if(sessionKeyOpt == uuidOpt){ return }
    sessionKeyOpt = uuidOpt
    instantTxManager.rwTx{ () ⇒ findOrAddSession() }
  }
  def comment = at.comment
  def handlers =
    CoHandler(SessionEventSource)(this) ::
    CoHandler(SwitchSession)(handleSessionKey) :: Nil
}
