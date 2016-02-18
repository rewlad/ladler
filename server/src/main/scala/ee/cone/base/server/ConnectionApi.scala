package ee.cone.base.server

import java.net.Socket

import ee.cone.base.connection_api.{Message, DictMessage}

trait SenderOfConnection {
  def send(event: String, data: String): Unit
}

trait ReceiverOfConnection {
  def connectionKey: String
}

trait LifeCycle {
  def setup[C](create: =>C)(close: C=>Unit): C
  def open(): Unit
  def close(): Unit
  def sub(): LifeCycle
  def sub[R](f: LifeCycle=>R): R
}

trait ConnectionRegistry {
  def send(bnd: DictMessage): Unit
}

case object PeriodicMessage extends Message

trait ReceiverOf[M] { def receive: PartialFunction[M,Unit] }

trait MixBase[Component] { // to utils?
  def args: List[Component]
  def createComponents() = args
  lazy val components = createComponents()
}

trait AppComponent
trait ConnectionComponent

trait CanStart {
  def start(): Unit
}

class SocketOfConnection(val value: Socket) extends ConnectionComponent