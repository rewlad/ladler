package io.github.rewlad.sseserver

trait SenderOfConnection extends Component {
  def send(event: String, data: String): Unit
}
trait ReceiverOfConnection extends Component {
  def connectionKey: String
  def poll(): Option[ConnectionRegistry.Message]
}
object ConnectionRegistry {
  type Message = Map[String,String]
}
trait FrameHandler extends Component {
  def frame(messageOption: Option[ConnectionRegistry.Message]): Unit
}
