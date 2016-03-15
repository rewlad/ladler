package ee.cone.base.db

import java.util.UUID
import java.util.concurrent.Future

import ee.cone.base.connection_api._

class CurrentRequest(var value: Option[UUID])

class MergerEventSourceOperationsImpl(
    ops: EventSourceOperations, at: MergerEventSourceAttrs,
    instantTxManager: DefaultTxManager[InstantEnvKey], mainTxManager: DefaultTxManager[MainEnvKey],
    uniqueNodes: UniqueNodes, currentRequest: CurrentRequest
) extends CoHandlerProvider {
  def setRequestOK(ok: Boolean): Unit = currentRequest.value.foreach{ uuid ⇒
    currentRequest.value = None
    instantTxManager.rwTx{ ()⇒
      ops.addEventStatus(uniqueNodes.whereSrcId(instantTxManager.currentTx(),uuid), ok)
    }
  }

  def handlers = CoHandler(ActivateReceiver){ ()=>
    //println("merger activated")
    setRequestOK(false)
    mainTxManager.rwTx { () ⇒
      instantTxManager.roTx { () ⇒
        val req = nextRequest()
        if (req.nonEmpty) {
          println("req nonEmpty")
          currentRequest.value = req(uniqueNodes.srcId)
          ops.applyEvents(req(at.instantSession), FindUpTo(req) :: Nil)
        }
      }
    }
    if(currentRequest.value.nonEmpty) setRequestOK(true) else Thread.sleep(1000)
    //? then notify
  } :: Nil
  private def nextRequest(): Obj = {
    val seqNode = uniqueNodes.seqNode(mainTxManager.currentTx())
    val seqRef = new Ref[Obj] {
      def apply() = seqNode(at.lastMergedRequest)
      def update(value: Obj) = seqNode(at.lastMergedRequest) = value
    }
    val reqSrc = ops.createEventSource(at.asRequest, at.requested, ops.requested, seqRef, Nil)
    reqSrc.poll()
  }
}

class Merger(
  lifeCycleManager: ExecutionManager,
  createConnection: LifeCycle ⇒ CoMixBase
) extends CanStart {
  def start() = lifeCycleManager.startServer { ()=>
    var activity: Option[Future[_]] = None
    while(true){
      if(activity.forall(_.isDone))
        activity = Option(lifeCycleManager.startConnection(createConnection))
      Thread.sleep(1000)
    }
  }
}