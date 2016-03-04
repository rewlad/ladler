package ee.cone.base.connection_api

import java.util.UUID

trait LifeCycle {
  def open(): Unit
  def close(): Unit
  def onClose(doClose: ()=>Unit): Unit
  def sub(): LifeCycle
}

trait CanStart {
  def start(): Unit
}

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

////

trait SenderOfConnection {
  def send(event: String, data: String): Unit
}

case object SwitchSession extends EventKey[UUID,Unit]

case object AlienDictMessageKey extends EventKey[Option[DictMessage], Unit]
case class DictMessage(value: Map[String,String])