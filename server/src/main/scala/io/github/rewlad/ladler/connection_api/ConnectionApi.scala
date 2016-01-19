package io.github.rewlad.ladler.connection_api

case class ReceivedMessage(value: Map[String,String])

object ActionOf {
  def apply(message: ReceivedMessage) = message.value.getOrElse("X-r-action","")
}

trait SenderOfConnection {
  def send(event: String, data: String): Unit
}

