package ee.cone.base.server

import java.util

import ee.cone.base.connection_api.ReceivedMessage
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

  private lazy val incoming = new util.ArrayDeque[ReceivedMessage]
  private def pollInner(): List[ReceivedMessage] = Option(incoming.poll()) match {
    case None => Nil
    case Some(m) => m :: pollInner()
  }
  def poll(): List[ReceivedMessage] = incoming.synchronized(pollInner())
  def add(message: ReceivedMessage) =
    incoming.synchronized(incoming.add(message))
}

class ConnectionRegistryImpl extends ConnectionRegistry {
  lazy val store = TrieMap[String, ReceiverOfConnectionImpl]()
  def send(bnd: ReceivedMessage) =
    store(bnd.value("X-r-connection")).add(bnd)
}
