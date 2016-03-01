package ee.cone.base.db

import ee.cone.base.db.Types._
import ee.cone.base.util.Single

class SessionEventSourceOperationsImpl(
  ops: EventSourceOperations, at: SessionEventSourceAttrs, instantTxStarter: TxManager, sessionState: SessionState
) extends SessionEventSourceOperations {
  private def findSessionId() = {
    val instantSession = Single(at.instantSessionsBySessionKey.list(sessionState.sessionKey))
    // or create?
    instantSession.objId
  }
  def incrementalApplyAndView[R](view: () => R) = {
    instantTxStarter.needTx(rw=false)
    ops.applyEvents(findSessionId(), (_:DBNode)=>false)
    val res = view()
    instantTxStarter.closeTx()
    res
  }
  def addEvent(label: Attr[Option[DBNode]])(fill: DBNode=>Unit): Unit = ops.addInstant(label){ ev =>
    ev(at.asEvent) = Some(ev)
    ev(at.sessionId) = Some(findSessionId())
    fill(ev)
  }
  def addRequest() = addEvent(at.asRequest)(_=>())
  def addUndo(eventObjId: ObjId) = ???
}
