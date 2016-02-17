package ee.cone.base.server

import java.util.concurrent.BlockingQueue

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
  def createReceiver(lifeTime: LifeCycle, queue: BlockingQueue[DictMessage]): ReceiverOfConnection
}

case object PeriodicMessage extends Message

trait ReceiverOf[M] { def receive: PartialFunction[M,Unit] }
