package ee.cone.base.connection_api

trait Message
case class DictMessage(value: Map[String,String]) extends Message

trait LifeCycled {
  def open(): Unit
  def close(): Unit
}
trait LifeCycle extends LifeCycled {
  def setup[C](create: =>C)(close: C=>Unit): C
}

trait AppComponent
trait ConnectionComponent
trait CanStart {
  def start(): Unit
}

trait Registration extends LifeCycled
