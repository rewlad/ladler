package ee.cone.base.db

import ee.cone.base.util.Never

class MergerEventSourceOperationsImpl(
  ops: EventSourceOperations, at: MergerEventSourceAttrs,
  mainTxManager: TxManager, instantTxStarter: TxManager,
  mainSeqNode: ()=>DBNode,
  instantValues: ListByValueStart
) extends MergerEventSourceOperations {
  def incrementalApplyAndCommit(): Unit = {
    mainTxManager.needTx(rw=true)
    instantTxStarter.needTx(rw=false)
    val seqRef = mainSeqNode()(at.unmergedRequestsFromId.ref)
    val reqSrc = ops.createEventSource(instantValues.of(at.asRequest.nonEmpty), true, seqRef)
    val reqOpt = reqSrc.poll()
    if(reqOpt.isEmpty) { return }
    val req = reqOpt.get
    var ok = false
    try {
      val sessionId = req(at.sessionId).get
      ops.applyEvents(sessionId, (ev:DBNode)=>
        if(ev.objId<req.objId) true else if(ev.objId==req.objId) false else Never()
      )
      instantTxStarter.closeTx()
      mainTxManager.commit()
      ok = true
    } finally {
      ops.addEventStatus(req, ok)
    }
    //? then notify
  }
}
