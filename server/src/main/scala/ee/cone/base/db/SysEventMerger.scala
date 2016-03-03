package ee.cone.base.db

import ee.cone.base.util.Never

class MergerEventSourceOperationsImpl(
  ops: EventSourceOperations, at: MergerEventSourceAttrs,
  mainTxManager: TxManager[MainEnvKey], instantTxStarter: TxManager[InstantEnvKey],
  nodeFactory: NodeFactory,
  instantNodes: DBNodes[InstantEnvKey]
) extends MergerEventSourceOperations {
  def incrementalApplyAndCommit(): Unit = {
    mainTxManager.needTx(rw=true)
    instantTxStarter.needTx(rw=false)
    val seqNode = nodeFactory.seqNode(mainTxManager.tx)
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
      instantTxStarter.closeTx()
      mainTxManager.commit()
      ok = true
    } finally {
      ops.addEventStatus(req, ok)
    }
    //? then notify
  }
}
