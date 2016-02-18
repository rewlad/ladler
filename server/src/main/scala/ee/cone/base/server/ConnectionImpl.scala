package ee.cone.base.server

import java.util
import java.util.concurrent.{BlockingQueue, LinkedBlockingQueue}

import ee.cone.base.connection_api.{Message, DictMessage}
import ee.cone.base.util.Setup

import scala.collection.concurrent.TrieMap

class ReceiverOfConnectionImpl(
  registry: ConnectionRegistryImpl, lifeTime: LifeCycle, queue: BlockingQueue[DictMessage] //Linked*
) extends ReceiverOfConnection {
  lazy val connectionKey = Setup(lifeTime.setup(util.UUID.randomUUID.toString){ k =>
    registry.store.remove(k)
    println(s"connection unregister: $k")
  }){ k =>
    registry.store(k) = queue
    println(s"connection   register: $k")
  }
}

class ConnectionRegistryImpl extends ConnectionRegistry {
  lazy val store = TrieMap[String, BlockingQueue[DictMessage]]()
  def send(bnd: DictMessage) = store(bnd.value("X-r-connection")).add(bnd)
}

////

class ActivateReceivers(
  queue: BlockingQueue[DictMessage], receivers: List[ReceiverOf[Message]]
) extends ReceiverOf[Message] {
  private def byAll(message: Message) = receivers.foreach(_.receive(message))
  def receive = { case message => next(message) }
  private def next(message: Message): Unit = Option(queue.poll()) match {
    case Some(m) =>
      byAll(m)
      next(message)
    case None => byAll(message)
  }
}
