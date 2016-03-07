package ee.cone.base.db

import java.util.concurrent.{ExecutorService, Executor}

import ee.cone.base.connection_api._
import ee.cone.base.db.Types.ObjId
import ee.cone.base.util.Never

class CurrentRequest(var value: Option[ObjId])

class MergerEventSourceOperationsImpl(
    ops: EventSourceOperations, at: MergerEventSourceAttrs,
    instantTxManager: DefaultTxManager[InstantEnvKey], mainTxManager: DefaultTxManager[MainEnvKey],
    nodeFactory: NodeFactory, currentRequest: CurrentRequest
) extends CoHandlerProvider {
  def setRequestOK(ok: Boolean): Unit = currentRequest.value.foreach{ objId ⇒
    currentRequest.value = None
    instantTxManager.rwTx{ ()⇒
      ops.addEventStatus(nodeFactory.toNode(instantTxManager.currentTx(),objId), ok)
    }
  }

  def handlers = CoHandler(ActivateReceiver){ _=>
    setRequestOK(false)
    mainTxManager.rwTx { () ⇒
      instantTxManager.roTx { () ⇒
        val req = nextRequest()
        if (req.nonEmpty) {
          currentRequest.value = Some(req.objId)
          applyEvents(req)
        } else Thread.sleep(1000)
      }
    }
    setRequestOK(true)
    //? then notify
  } :: Nil
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

class Merger(
  lifeCycleManager: ExecutionManager,
  createConnection: LifeCycle ⇒ CoMixBase
) extends CanStart {
  def start() = lifeCycleManager.startServer { ()=>
    lifeCycleManager.startConnection { lifeCycle =>
      createConnection(lifeCycle)
    }.get()
    Thread.sleep(1000)
  }
}