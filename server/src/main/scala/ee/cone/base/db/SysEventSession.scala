package ee.cone.base.db

import ee.cone.base.db.Types._
import ee.cone.base.util.Single

class SessionEventSourceOperationsImpl(
  ops: EventSourceOperations, at: SessionEventSourceAttrs,
  instantTxStarter: TxManager[InstantEnvKey], sessionState: SessionState,
  mainValues: ListByValueStart[MainEnvKey]
) extends SessionEventSourceOperations {
  private def findSession() = // or create?
    Single(mainValues.of(at.asInstantSession.defined, at.sessionKey).list(sessionState.sessionKey))
  def incrementalApplyAndView[R](view: () => R) = {
    instantTxStarter.needTx(rw=false)
    ops.applyEvents(findSession(), (_:DBNode)=>false)
    val res = view()
    instantTxStarter.closeTx()
    res
  }
  def addEvent(fill: DBNode=>Unit): Unit = ops.addInstant(at.asEvent){ ev =>
    ev(at.instantSession) = findSession()
    fill(ev)
  }
  def addRequest() = addEvent(ev=> ev(at.requested)=ops.requested)
  def addUndo(eventObjId: ObjId) = ???
}
