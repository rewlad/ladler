package io.github.rewlad.sseserver

trait SenderOfConnection extends Component {
  def send(event: String, data: String): Unit
}
trait ReceiverOfConnection extends Component {
  def connectionKey: String
  def messageOption: Option[ReceiverOfConnection.Message]
  def poll(): Unit
}
object ReceiverOfConnection {
  type Message = Map[String,String]
}
trait FrameHandler extends Component {
  def frame(): Unit
}
