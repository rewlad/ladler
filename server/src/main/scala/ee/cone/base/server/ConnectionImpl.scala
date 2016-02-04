package ee.cone.base.server

import java.util

import ee.cone.base.connection_api.{Message, DictMessage}
import ee.cone.base.util.Setup

import scala.collection.concurrent.TrieMap

class ReceiverOfConnectionImpl(lifeTime: LifeCycle, registry: ConnectionRegistryImpl) extends ReceiverOfConnection {
  private def createConnectionKey = Setup(util.UUID.randomUUID.toString){ k =>
    registry.store(k) = this
    println(s"connection   register: $k")
  }
  lazy val connectionKey = lifeTime.setup(createConnectionKey){ k =>
    registry.store.remove(k)
    println(s"connection unregister: $k")
  }

  private lazy val incoming = new util.ArrayDeque[DictMessage]
  def poll(): Option[DictMessage] =
    incoming.synchronized(Option(incoming.poll()))
  def add(message: DictMessage) =
    incoming.synchronized(incoming.add(message))
}

class ConnectionRegistryImpl extends ConnectionRegistry {
  lazy val store = TrieMap[String, ReceiverOfConnectionImpl]()
  def send(bnd: DictMessage) =
    store(bnd.value("X-r-connection")).add(bnd)
}

class ActivateReceivers(
  receiver: ReceiverOfConnectionImpl, receivers: List[ReceiverOf[Message]]
) extends ReceiverOf[Message] {
  private def byAll(message: Message) = receivers.foreach(_.receive(message))
  def receive = { case message => next(message) }
  private def next(message: Message): Unit = receiver.poll() match {
    case Some(m) =>
      byAll(m)
      next(message)
    case None => byAll(message)
  }
}
