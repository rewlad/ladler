package ee.cone.base.db

import ee.cone.base.db.Types._
import ee.cone.base.util.Single

class SessionEventSourceOperationsImpl(
  ops: EventSourceOperations, at: SessionEventSourceAttrs,
  instantTxStarter: TxManager[InstantEnvKey], sessionState: SessionState,
  mainNodes: DBNodes[MainEnvKey]
) extends SessionEventSourceOperations {
  private def findSession() = // or create?
    Single(mainNodes.where(at.asInstantSession.defined, at.sessionKey, sessionState.sessionKey))
  def incrementalApplyAndView[R](view: () => R) = {
    instantTxStarter.needTx(rw=false)
    ops.applyEvents(findSession(), (_:DBNode)=>false)
    val res = view()
    instantTxStarter.closeTx()
    res
  }
  def addEvent(fill: DBNode=>Unit): Unit = ops.addEvent(findSession(), fill)



  def addRequest() = addEvent(ev=> ev(at.requested)=ops.requested)
  def addUndo(eventObjId: ObjId) = ???
}
