package ee.cone.base.db

import java.util.concurrent.Future

import ee.cone.base.connection_api._

// lifetime of mergerCurrentRequest should be longer than merger's one
// ObjId is here, because Obj can not live longer than connection, it is bound to handlers
class CurrentRequest(var objId: ObjId)

class MergerEventSourceOperationsImpl(
  ops: ForMergerEventSourceOperations,
  objIdFactory: ObjIdFactory,
  nodeAttrs: NodeAttrs,
  instantTxManager: DefaultTxManager[InstantEnvKey],
  mainTxManager: DefaultTxManager[MainEnvKey],
  findNodes: FindNodes, currentRequest: CurrentRequest
) extends CoHandlerProvider {
  def handlers = CoHandler(ActivateReceiver){ ()=>
    if(currentRequest.objId.nonEmpty){
      val objId = currentRequest.objId
      currentRequest.objId = objIdFactory.noObjId
      instantTxManager.rwTx{ ()⇒ ops.undo(findNodes.whereObjId(objId)) }
    }
    println("currentRequest before")
    mainTxManager.rwTx { () ⇒
      instantTxManager.roTx { () ⇒
        val req = ops.nextRequest()
        currentRequest.objId = req(nodeAttrs.objId)
        if (currentRequest.objId.nonEmpty) ops.applyRequestedEvents(req)
      }
    }
    println("currentRequest after")
    if(currentRequest.objId.nonEmpty){
      val objId = currentRequest.objId
      instantTxManager.rwTx { () ⇒ ops.addCommit(findNodes.whereObjId(objId)) }
      currentRequest.objId = objIdFactory.noObjId
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