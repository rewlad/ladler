package ee.cone.base.server_impl

import java.io.OutputStream

import ee.cone.base.connection_api._

trait ReceiverOfConnection extends {
  def connectionKey: String
}
trait ConnectionRegistry {
  def send(bnd: DictMessage): Unit
}

case object SetOutput extends EventKey[OutputStream=>Unit]
