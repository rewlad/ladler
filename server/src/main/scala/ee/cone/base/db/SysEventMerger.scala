package ee.cone.base.db

import java.util.UUID
import java.util.concurrent.Future

import ee.cone.base.connection_api._

class CurrentRequest(var value: Obj)

class MergerEventSourceOperationsImpl(
    ops: ForMergerEventSourceOperations, nodeAttrs: NodeAttrs,
    instantTxManager: DefaultTxManager[InstantEnvKey], mainTxManager: DefaultTxManager[MainEnvKey],
    uniqueNodes: UniqueNodes
)(
    var currentRequest: Obj = uniqueNodes.noNode
) extends CoHandlerProvider {
  def handlers = CoHandler(ActivateReceiver){ ()=>
    if(currentRequest(nodeAttrs.nonEmpty)) {
      val req = currentRequest
      currentRequest = uniqueNodes.noNode
      instantTxManager.rwTx{ ()⇒ ops.undo(req) }
    }
    mainTxManager.rwTx { () ⇒
      instantTxManager.roTx { () ⇒
        currentRequest = ops.nextRequest()
        if (currentRequest(nodeAttrs.nonEmpty))
          ops.applyRequestedEvents(currentRequest)
      }
    }
    if(currentRequest(nodeAttrs.nonEmpty)) {
      instantTxManager.rwTx { () ⇒ ops.addCommit(currentRequest) }
      currentRequest = uniqueNodes.noNode
    } else Thread.sleep(1000)
    //? then notify
  } :: Nil
}

class Merger(
  lifeCycleManager: ExecutionManager,
  createConnection: LifeCycle ⇒ CoMixBase
) extends CanStart {
  def start() = lifeCycleManager.submit { ()=>
    var activity: Option[Future[_]] = None
    while(true){
      if(activity.forall(_.isDone))
        activity = Option(lifeCycleManager.startConnection(createConnection))
      Thread.sleep(1000)
    }
  }
}