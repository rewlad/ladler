package ee.cone.base.db

import java.util.UUID
import java.util.concurrent.Future

import ee.cone.base.connection_api._

class CurrentRequest(var value: Option[UUID])

class MergerEventSourceOperationsImpl(
    ops: ForMergerEventSourceOperations,
    instantTxManager: DefaultTxManager[InstantEnvKey], mainTxManager: DefaultTxManager[MainEnvKey],
    uniqueNodes: UniqueNodes, currentRequest: CurrentRequest
) extends CoHandlerProvider {
  private def toInstantNode(uuid: UUID) =
    uniqueNodes.whereSrcId(instantTxManager.currentTx(),uuid)

  def handlers = CoHandler(ActivateReceiver){ ()=>
    currentRequest.value.foreach { uuid ⇒
      currentRequest.value = None
      instantTxManager.rwTx{ ()⇒ ops.undo(toInstantNode(uuid)) }
    }
    mainTxManager.rwTx { () ⇒
      instantTxManager.roTx { () ⇒
        val req = ops.nextRequest()
        if (req.nonEmpty) {
          println("merger: req nonEmpty")
          currentRequest.value = req(uniqueNodes.srcId)
          ops.applyRequestedEvents(req)
        }
      }
    }
    if(currentRequest.value.nonEmpty) {
      instantTxManager.rwTx { () ⇒
        ops.addCommit (toInstantNode(currentRequest.value.get))
      }
      currentRequest.value = None
    } else Thread.sleep(1000)
    //? then notify
  } :: Nil
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