package ee.cone.base.connection_api

trait Message
case class DictMessage(value: Map[String,String]) extends Message

trait LifeCycled {
  def open(): Unit
  def close(): Unit
}
trait AliveValue[Value] {
  def value: Value
  def onClose(doClose: Value=>Unit): this.type
  def updates(set: Option[Value]=>Unit): this.type
}
trait LifeCycle extends LifeCycled {
  def onClose(doClose: ()=>Unit): Unit
  def of[Value](create: ()=>Value): AliveValue[Value]
  def sub(): LifeCycle
}

trait AppComponent
trait ConnectionComponent
trait CanStart {
  def start(): Unit
}
