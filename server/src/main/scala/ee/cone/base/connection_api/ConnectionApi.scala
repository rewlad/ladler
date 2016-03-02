package ee.cone.base.connection_api

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
trait CoHandler[-In,+Out] extends BaseCoHandler {
  def on: List[EventKey[In,Out]]
  def handle(in: In): Out
}
trait CoHandlerLists {
  def list[In,Out](ev: EventKey[In,Out]): List[CoHandler[In,Out]]
}