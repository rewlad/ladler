package io.github.rewlad.ladler.server

trait SenderOfConnection {
  def send(event: String, data: String): Unit
}
trait ReceiverOfConnection {
  def connectionKey: String
  def poll(): Option[ReceivedMessage]
}
case class ReceivedMessage(value: Map[String,String])
trait FrameHandler {
  def frame(messageOption: Option[ReceivedMessage]): Unit
}
