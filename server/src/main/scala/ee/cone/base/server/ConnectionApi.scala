package ee.cone.base.server

import java.io.OutputStream
import java.net.Socket
import java.util.concurrent.BlockingQueue

import ee.cone.base.connection_api._



trait ReceiverOfConnection extends {
  def connectionKey: String
}
trait ConnectionRegistry {
  def send(bnd: DictMessage): Unit
}

case object SetOutput extends EventKey[OutputStream,Unit]
case object ActivateReceiver extends EventKey[Unit,Unit]
case object FailEventKey extends EventKey[Exception,Unit]

