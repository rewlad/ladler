package ee.cone.base.db_impl

import java.util.UUID
import ee.cone.base.connection_api._
import ee.cone.base.db.Types._
import ee.cone.base.util.{Setup, Never, Single}

class SessionEventSourceOperationsImpl(
  ops: ForSessionEventSourceOperations,
  at: SessionEventSourceAttrs, nodeAttrs: NodeAttrs, sysAttrs: FindAttrs,
  handlerLists: CoHandlerLists,
  instantTxManager: DefaultTxManager[InstantEnvKey], mainTxManager: SessionMainTxManager,
  findNodes: FindNodes
)(
    var lastStatus: Obj = findNodes.noNode
) extends SessionEventSourceOperations with CoHandlerProvider {
  private var sessionKeyOpt: Option[UUID] = None
  def sessionKey = sessionKeyOpt.get
  def findSession(): Obj = {
    val tx = instantTxManager.currentTx()
    findNodes.single(findNodes.where(tx, ops.findInstantSessionBySessionKey, sessionKeyOpt, Nil))
  }
  def mainSession = findSession()(at.mainSession)
  private def addSession() = {
    val tx = instantTxManager.currentTx()
    val instantSession = ops.addInstant(findNodes.noNode, at.asInstantSession)
    instantSession(at.sessionKey) = sessionKeyOpt
    instantSession(at.mainSession) = findNodes.whereObjId(findNodes.toObjId(UUID.randomUUID))
  }
  def incrementalApplyAndView[R](view: () => R) = {
    instantTxManager.roTx { () ⇒
      val instantSession = findSession()
      mainTxManager.muxTx(needRecreate(instantSession)){ () ⇒
        ops.applyEvents(instantSession)
        view()
      }
    }
  }
  def unmergedEvents = ops.unmergedEvents(findSession())
  var decoupled: Boolean = false
  private def needRecreate(instantSession: Obj): Boolean = {
    val tx = instantTxManager.currentTx()
    val findAfter = if(lastStatus(sysAttrs.nonEmpty)) List(FindAfter(lastStatus)) else Nil
    val options = FindLastOnly :: findAfter
    val newStatus = findNodes.single(if(decoupled) findNodes.where(
      tx, ops.findCommitByInstantSession, instantSession, options
    ) else findNodes.where(
      tx, ops.findCommit, findNodes.zeroNode, options
    ))
    if(!newStatus(sysAttrs.nonEmpty)) return false
    lastStatus = newStatus
    true
  }
  def addInstantTx(f: () ⇒ Boolean): Unit = if(instantTxManager.rwTx(f))
    handlerLists.list(SessionInstantAdded).foreach(_())
  def addUndo(event: Obj) = addInstantTx { () ⇒
    val instantSession = findSession()
    val objId = event(nodeAttrs.objId)
    mainTxManager.muxTx(recreate=true) { () =>
      var undo = true
      var added = false
      ops.unmergedEvents(instantSession).reverse.foreach { ev =>
        if(ev(at.applyAttr) == at.requested) Never() // check
        if(undo){
          ops.undo(ev)
          added = true
        }
        if(ev(nodeAttrs.objId) == objId) undo = false
      }
      added
    }
  }
  def addEvent(fill: Obj=>ObjId): Unit = addInstantTx { () ⇒
    val instantSession = findSession()
    val ev = ops.addInstant(instantSession, at.asEvent)
    ev(at.applyAttr) = fill(ev)
    true
  }

  def addRequest() = addEvent{ req ⇒ at.requested }
  private def handleSessionKey(uuid: UUID): Unit = {
    val uuidOpt = Option(uuid)
    if(sessionKeyOpt == uuidOpt){ return }
    sessionKeyOpt = uuidOpt
    addInstantTx{ () ⇒
      if(findSession()(sysAttrs.nonEmpty)) false else { addSession(); true }
    }
  }
  def handlers =
    CoHandler(SessionEventSource)(this) ::
    CoHandler(SwitchSession)(handleSessionKey) :: Nil
}
