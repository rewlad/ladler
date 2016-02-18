package ee.cone.base.server

import java.net.Socket
import java.util.concurrent.BlockingQueue

import ee.cone.base.connection_api._

trait SenderOfConnection {
  def send(event: String, data: String): Unit
}

trait ReceiverOfConnection extends {
  def connectionKey: String
  def queue: BlockingQueue[DictMessage]
}
trait ConnectionRegistry {
  def send(bnd: DictMessage): Unit
}

case object PeriodicMessage extends Message

trait ReceiverOf[M] { def receive: PartialFunction[M,Unit] }

class SocketOfConnection(val value: Socket) extends ConnectionComponent




