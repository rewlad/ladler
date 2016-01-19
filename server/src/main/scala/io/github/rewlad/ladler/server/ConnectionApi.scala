package io.github.rewlad.ladler.server

import io.github.rewlad.ladler.connection_api.ReceivedMessage

trait ReceiverOfConnection {
  def connectionKey: String
  def poll(): Option[ReceivedMessage]
}

trait FrameHandler {
  def frame(messageOption: Option[ReceivedMessage]): Unit
}

trait LifeCycle {
  def setup[C](create: =>C)(close: C=>Unit): C
  def open(): Unit
  def close(): Unit
}

trait ConnectionRegistry {
  def send(bnd: ReceivedMessage): Unit
}