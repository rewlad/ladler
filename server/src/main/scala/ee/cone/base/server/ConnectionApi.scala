package ee.cone.base.server

import ee.cone.base.connection_api.ReceivedMessage

trait SenderOfConnection {
  def send(event: String, data: String): Unit
}

trait ReceiverOfConnection {
  def connectionKey: String
  def poll(): List[ReceivedMessage]
}

trait FrameHandler {
  def frame(messages: List[ReceivedMessage]): Unit
}

trait LifeCycle {
  def setup[C](create: =>C)(close: C=>Unit): C
  def open(): Unit
  def close(): Unit
}

trait ConnectionRegistry {
  def send(bnd: ReceivedMessage): Unit
}