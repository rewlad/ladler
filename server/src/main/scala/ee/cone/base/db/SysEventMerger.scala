package ee.cone.base.db

import ee.cone.base.db.Types.ObjId
import ee.cone.base.util.Never

class CurrentRequest(var value: Option[ObjId])

class MergerEventSourceOperationsImpl(
    ops: EventSourceOperations, at: MergerEventSourceAttrs,
    instantTxManager: DefaultTxManager[InstantEnvKey], mainTxManager: DefaultTxManager[MainEnvKey],
    nodeFactory: NodeFactory, currentRequest: CurrentRequest
) extends MergerEventSourceOperations {
  def setRequestOK(ok: Boolean): Unit = currentRequest.value.foreach{ objId ⇒
    currentRequest.value = None
    instantTxManager.rwTx{ ()⇒
      ops.addEventStatus(nodeFactory.toNode(instantTxManager.currentTx(),objId), ok)
    }
  }

  def incrementalApplyAndCommit(): Unit = {
    setRequestOK(false)
    mainTxManager.rwTx { () ⇒
      instantTxManager.roTx { () ⇒
        val req = nextRequest()
        if (req.nonEmpty) {
          currentRequest.value = Some(req.objId)
          applyEvents(req)
        }
      }
    }
    setRequestOK(true)
    //? then notify
  }
  private def nextRequest(): DBNode = {
    val seqNode = nodeFactory.seqNode(mainTxManager.currentTx())
    val seqRef: Ref[DBNode] = seqNode(at.lastMergedRequest.ref)
    val reqSrc = ops.createEventSource(at.asRequest, at.requested, ops.requested, seqRef)
    reqSrc.poll()
  }
  private def applyEvents(req: DBNode) = {
    val instantSession = req(at.instantSession)
    ops.applyEvents(instantSession, (ev:DBNode)=>
      if(ev.objId<req.objId) true else if(ev.objId==req.objId) false else Never()
    )
  }

}
