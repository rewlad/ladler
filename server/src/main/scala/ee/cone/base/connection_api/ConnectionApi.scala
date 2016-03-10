package ee.cone.base.connection_api

import java.util.UUID
import java.util.concurrent.{ExecutorService, Future}

// lifecycle of connection and personal sub-objects with shorter life
trait LifeCycle {
  def close(): Unit
  def onClose(doClose: ()=>Unit): Unit
  def sub(): LifeCycle
}

// common interfaces to communicate between decoupled components of connection;
trait EventKey[-In,+Out]
trait BaseCoHandler
case class CoHandler[In,Out](on: EventKey[In,Out])(val handle: In=>Out)
  extends BaseCoHandler

trait CoHandlerLists {
  def list[In,Out](ev: EventKey[In,Out]): List[In=>Out]
}
trait CoHandlerProvider {
  def handlers: List[BaseCoHandler]
}
trait CoMixBase extends CoHandlerProvider {
  def handlerLists: CoHandlerLists
  def handlers: List[BaseCoHandler] = Nil
}

// Single shared app object of a project gathers all shared app-level components;
// app extends AppMixBase;
// call app.start() in main method of a project;
// override app.toStart to include components to be started;
// implement CanStart.start() in those components;
// startServer(...) can be used inside start();
// startConnection(...) can be used inside startServer(...);
trait CanStart {
  def start(): Unit
}
trait ExecutionManager {
  def pool: ExecutorService
  def startServer(iteration: ()=>Unit): Unit
  def startConnection(setup: LifeCycle=>CoMixBase): Future[_]
}
trait AppMixBase extends CanStart {
  def toStart: List[CanStart] = Nil
  def executionManager: ExecutionManager
}

////////////////////////////////

trait BoundToTx
trait Obj {
  def nonEmpty: Boolean
  def apply[Value](attr: Attr[Value]): Value
  def update[Value](attr: Attr[Value], value: Value): Unit
  def tx: BoundToTx
}
trait Attr[Value] {
  def defined: Attr[Boolean]
  def get(node: Obj): Value
  def set(node: Obj, value: Value): Unit
}

////////////////////////////////

// subscribe to implement iteration of connection
case object ActivateReceiver extends EventKey[Unit,Unit]
// subscribe to failures (fatal) of connection
case object FailEventKey extends EventKey[Exception,Unit]

case object SwitchSession extends EventKey[UUID,Unit]

// exchange with alien (user agent)
case object FromAlienDictMessage extends EventKey[DictMessage, Unit]
case class DictMessage(value: Map[String,String])
case object ShowToAlien extends EventKey[Unit,List[(String,String)]]

case class ChangeEventAdder[Value](attr: Attr[Value]) extends EventKey[Obj,Value=>Unit]
