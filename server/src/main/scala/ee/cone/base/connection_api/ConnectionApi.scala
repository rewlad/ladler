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
trait EventKey[Item]
trait BaseCoHandler
case class CoHandler[Item](on: EventKey[Item])(val handle: Item)
  extends BaseCoHandler

trait CoHandlerLists {
  def list[Item](ev: EventKey[Item]): List[Item]
  def single[Item](ev: EventKey[Item], fail: ()⇒Item): Item
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
  def submit(run: ()=>Unit): Future[_]
  def startConnection(setup: LifeCycle=>CoMixBase): Future[_]
}
trait AppMixBase extends CanStart {
  def toStart: List[CanStart] = Nil
  def executionManager: ExecutionManager
}

////////////////////////////////

trait WrapType[WrapData]
trait Obj {
  def apply[Value](attr: Attr[Value]): Value
  def update[Value](attr: Attr[Value], value: Value): Unit
  def wrap[FWrapData](wrapType: WrapType[FWrapData], wrapData: FWrapData): Obj
}
trait Attr[Value]

////////////////////////////////

// subscribe to implement iteration of connection
case object ActivateReceiver extends EventKey[()=>Unit]
// subscribe to failures (fatal) of connection
case object FailEventKey extends EventKey[Exception=>Unit]

case object SwitchSession extends EventKey[UUID=>Unit]

// exchange with alien (user agent)
case object FromAlienDictMessage extends EventKey[DictMessage=>Unit]
case class DictMessage(value: Map[String,String])
case object ShowToAlien extends EventKey[()=>List[(String,String)]]

case class AddCreateEvent(labelAttr: Attr[Boolean]) extends EventKey[UUID=>Unit]
