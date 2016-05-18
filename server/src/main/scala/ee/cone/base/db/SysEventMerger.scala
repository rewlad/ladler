package ee.cone.base.db

import java.util.concurrent.Future

import ee.cone.base.connection_api._

class CurrentRequest(var value: Obj)

class MergerEventSourceOperationsImpl(
    ops: ForMergerEventSourceOperations, findAttrs: FindAttrs,
    instantTxManager: DefaultTxManager[InstantEnvKey], mainTxManager: DefaultTxManager[MainEnvKey],
  findNodes: FindNodes
)(
    var currentRequest: Obj = findNodes.noNode
) extends CoHandlerProvider {
  def handlers = CoHandler(ActivateReceiver){ ()=>
    if(currentRequest(findAttrs.nonEmpty)) {
      val req = currentRequest
      currentRequest = findNodes.noNode
      instantTxManager.rwTx{ ()⇒ ops.undo(req) }
    }
    mainTxManager.rwTx { () ⇒
      instantTxManager.roTx { () ⇒
        currentRequest = ops.nextRequest()
        if (currentRequest(findAttrs.nonEmpty))
          ops.applyRequestedEvents(currentRequest)
      }
    }
    if(currentRequest(findAttrs.nonEmpty)) {
      instantTxManager.rwTx { () ⇒ ops.addCommit(currentRequest) }
      currentRequest = findNodes.noNode
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