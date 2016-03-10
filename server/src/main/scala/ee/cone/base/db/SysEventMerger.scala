package ee.cone.base.db

import java.util.UUID
import java.util.concurrent.{ExecutorService, Executor}

import ee.cone.base.connection_api._
import ee.cone.base.db.Types.ObjId
import ee.cone.base.util.Never

class CurrentRequest(var value: Option[UUID])

class MergerEventSourceOperationsImpl(
    ops: EventSourceOperations, at: MergerEventSourceAttrs,
    instantTxManager: DefaultTxManager[InstantEnvKey], mainTxManager: DefaultTxManager[MainEnvKey],
    allNodes: DBNodes, currentRequest: CurrentRequest
) extends CoHandlerProvider {
  def setRequestOK(ok: Boolean): Unit = currentRequest.value.foreach{ uuid ⇒
    currentRequest.value = None
    instantTxManager.rwTx{ ()⇒
      ops.addEventStatus(allNodes.whereSrcId(instantTxManager.currentTx(),uuid), ok)
    }
  }

  def handlers = CoHandler(ActivateReceiver){ _=>
    setRequestOK(false)
    mainTxManager.rwTx { () ⇒
      instantTxManager.roTx { () ⇒
        val req = nextRequest()
        if (req.nonEmpty) {
          currentRequest.value = req(allNodes.srcId)
          ops.applyEvents(req(at.instantSession), FindUpTo(req) :: Nil)
        } else Thread.sleep(1000)
      }
    }
    setRequestOK(true)
    //? then notify
  } :: Nil
  private def nextRequest(): Obj = {
    val seqNode = allNodes.seqNode(mainTxManager.currentTx())
    val seqRef: Ref[Obj] = ops.ref(seqNode, at.lastMergedRequest)
    val reqSrc = ops.createEventSource(at.asRequest, at.requested, ops.requested, seqRef, Nil)
    reqSrc.poll()
  }
}

class Merger(
  lifeCycleManager: ExecutionManager,
  createConnection: LifeCycle ⇒ CoMixBase
) extends CanStart {
  def start() = lifeCycleManager.startServer { ()=>
    lifeCycleManager.startConnection(createConnection).get()
    Thread.sleep(1000)
  }
}