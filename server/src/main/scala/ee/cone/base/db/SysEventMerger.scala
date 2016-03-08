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
          ops.applyEvents(req(at.instantSession), req.objId)
        } else Thread.sleep(1000)
      }
    }
    setRequestOK(true)
    //? then notify
  } :: Nil
  private def nextRequest(): DBNode = {
    val seqNode = nodeFactory.seqNode(mainTxManager.currentTx())
    val seqRef: Ref[DBNode] = seqNode(at.lastMergedRequest.ref)
    val reqSrc = ops.createEventSource(at.asRequest, at.requested, ops.requested, seqRef, Long.MaxValue)
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