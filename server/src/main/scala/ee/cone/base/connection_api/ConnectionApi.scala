package ee.cone.base.connection_api

import java.util.UUID

trait Message
case class DictMessage(value: Map[String,String]) extends Message

trait AliveValue[Value] {
  def value: Value
  def onClose(doClose: Value=>Unit): this.type
  def updates(set: Option[Value]=>Unit): this.type
}
trait LifeCycle {
  def open(): Unit
  def close(): Unit
  def onClose(doClose: ()=>Unit): Unit
  def of[Value](create: ()=>Value): AliveValue[Value]
  def sub(): LifeCycle
}

trait CanStart {
  def start(): Unit
}

trait EventKey[-In,+Out]
trait BaseCoHandler
case class CoHandler[In,Out](on: List[EventKey[In,Out]])(val handle: In=>Out)
  extends BaseCoHandler

trait CoHandlerLists {
  def list[In,Out](ev: EventKey[In,Out]): List[In=>Out]
}
trait CoHandlerProvider {
  def handlers: List[BaseCoHandler]
}

////

case object SwitchSession extends EventKey[UUID,Unit]