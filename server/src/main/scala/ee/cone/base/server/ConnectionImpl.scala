package ee.cone.base.server

import java.util
import java.util.concurrent.BlockingQueue

import ee.cone.base.connection_api._
import scala.collection.concurrent.TrieMap

class ReceiverOfConnectionImpl(
  registry: ConnectionRegistry, val queue: BlockingQueue[DictMessage] //Linked*
) extends ReceiverOfConnection {
  def registries = registry :: Nil
  lazy val connectionKey = util.UUID.randomUUID.toString
}

class ConnectionRegistryImpl extends ConnectionRegistry {
  lazy val store = TrieMap[String, ReceiverOfConnection]()
  def send(bnd: DictMessage) = store(bnd.value("X-r-connection")).queue.add(bnd)
}

class ConnectionRegistration(
  registry: ConnectionRegistryImpl, item: ReceiverOfConnection
) extends CoHandler[LifeCycle,Unit] {
  def on = ConnectionRegistrationEventKey :: Nil
  def handle(lifeCycle: LifeCycle): Unit = {
    lifeCycle.onClose{()=>
      val k = item.connectionKey
      registry.store(k) = item
      println(s"connection   register: $k")
    }
    val k = item.connectionKey
    registry.store.remove(k)
    println(s"connection unregister: $k")
  }
}




////
/*
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
*/