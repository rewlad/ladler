package ee.cone.base.server

import java.net.Socket
import java.util.concurrent.BlockingQueue

import ee.cone.base.connection_api._

trait SenderOfConnection {
  def send(event: String, data: String): Unit
}

trait ReceiverOfConnection extends {
  def connectionKey: String
  def activate(): Unit
}
trait ConnectionRegistry {
  def send(bnd: DictMessage): Unit
}

case object PeriodicMessage extends EventKey[Unit,Unit]
case object AlienDictMessageKey extends EventKey[DictMessage, Unit]

class SocketOfConnection(val value: Socket)

case object ConnectionRegistrationEventKey extends EventKey[LifeCycle, Unit]


