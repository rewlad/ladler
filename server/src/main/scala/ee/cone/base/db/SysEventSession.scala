package ee.cone.base.db

import java.util.UUID
import ee.cone.base.connection_api._
import ee.cone.base.db.Types._
import ee.cone.base.util.{Never, Single}

class SessionEventSourceOperationsImpl(
  ops: ForSessionEventSourceOperations, at: SessionEventSourceAttrs, sysAttrs: SysAttrs,
  instantTxManager: DefaultTxManager[InstantEnvKey], mainTxManager: SessionMainTxManager,
  findNodes: FindNodes, uniqueNodes: UniqueNodes
) extends SessionEventSourceOperations with CoHandlerProvider {
  private var sessionKeyOpt: Option[UUID] = None
  def sessionKey = sessionKeyOpt.get
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
        ops.applyEvents(instantSession)
        view()
      }
    }
  }
  def unmergedEvents = ops.unmergedEvents(findSession().get)
  var decoupled: Boolean = false
  private var lastStatusSrcId: Option[UUID] = None
  private def needRecreate(instantSession: Obj): Boolean = {
    val tx = instantTxManager.currentTx()
    val options =
      FindLastOnly :: lastStatusSrcId.map(uuid=>FindAfter(uniqueNodes.whereSrcId(tx, uuid))).toList
    val newStatuses = if(decoupled) findNodes.where(
      tx, at.asCommit.defined, at.instantSession, instantSession, options
    ) else findNodes.where(
      tx, at.asCommit.defined, sysAttrs.justIndexed, findNodes.justIndexed, options
    )
    if(newStatuses.isEmpty) return false
    val newStatus :: Nil = newStatuses
    lastStatusSrcId = newStatus(uniqueNodes.srcId)
    true
  }
  def addUndo(uuid: UUID) = instantTxManager.rwTx { () ⇒
    val instantSession = findSession().get
    mainTxManager.muxTx(recreate=true)(()=>
      ops.unmergedEvents(instantSession).reverse.foreach{ ev =>
        if(ev(uniqueNodes.srcId).get == uuid) ops.undo(ev)
        if(ev(at.applyAttr) == at.requested) Never() // check
      }
    )
  }
  def addEvent(fill: Obj=>(Attr[Boolean],String)): Unit = instantTxManager.rwTx { () ⇒
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
