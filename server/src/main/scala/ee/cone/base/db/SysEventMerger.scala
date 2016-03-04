package ee.cone.base.db

import ee.cone.base.util.Never

class MergerEventSourceOperationsImpl(
    ops: EventSourceOperations, at: MergerEventSourceAttrs,
    instantTxManager: TxManager[InstantEnvKey], mainTxManager: TxManager[MainEnvKey],
    nodeFactory: NodeFactory, mainTx: CurrentTx[MainEnvKey]
) extends MergerEventSourceOperations {
  def incrementalApplyAndCommit(): Unit = {
    mainTxManager.needTx(rw=true)
    instantTxManager.needTx(rw=false)
    val seqNode = nodeFactory.seqNode(mainTx())
    val seqRef: Ref[DBNode] = seqNode(at.lastMergedRequest.ref)
    val reqSrc = ops.createEventSource(at.asRequest, at.requested, ops.requested, seqRef)
    val req = reqSrc.poll()
    if(!req.nonEmpty) { return }
    var ok = false
    try {
      val instantSession = req(at.instantSession)
      ops.applyEvents(instantSession, (ev:DBNode)=>
        if(ev.objId<req.objId) true else if(ev.objId==req.objId) false else Never()
      )
      instantTxManager.closeTx()
      mainTxManager.commit()
      ok = true
    } finally {
      instantTxManager.closeTx()
      instantTxManager.needTx(rw=true)
      ops.addEventStatus(req, ok)
      instantTxManager.commit()
    }
    //? then notify
  }
}
