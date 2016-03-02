package ee.cone.base.db

import ee.cone.base.db.Types._
import ee.cone.base.util.Single

class SessionEventSourceOperationsImpl(
  ops: EventSourceOperations, at: SessionEventSourceAttrs,
  instantTxStarter: TxManager[InstantEnvKey], sessionState: SessionState,
  mainValues: ListByValueStart[MainEnvKey]
) extends SessionEventSourceOperations {
  private def findSessionId() = {
    val instantSession = Single(mainValues.of(at.asInstantSession.nonEmpty, at.sessionKey).list(Some(sessionState.sessionKey)))
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
