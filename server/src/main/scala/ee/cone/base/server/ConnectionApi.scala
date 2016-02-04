package ee.cone.base.server

import ee.cone.base.connection_api.{Message, DictMessage}

trait SenderOfConnection {
  def send(event: String, data: String): Unit
}

trait ReceiverOfConnection {
  def connectionKey: String
}

trait LifeCycle {
  def setup[C](create: =>C)(close: C=>Unit): C
  def open(): Unit
  def close(): Unit
}

trait ConnectionRegistry {
  def send(bnd: DictMessage): Unit
}

case object PeriodicMessage extends Message

trait ReceiverOf[M] { def receive: PartialFunction[M,Unit] }
