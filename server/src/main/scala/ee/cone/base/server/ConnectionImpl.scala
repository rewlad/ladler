package ee.cone.base.server

import java.util
import java.util.concurrent.{TimeUnit, LinkedBlockingQueue, BlockingQueue}

import ee.cone.base.connection_api._
import scala.collection.concurrent.TrieMap

class ReceiverOfConnectionImpl(
    handlerLists: CoHandlerLists,
    registry: ConnectionRegistryImpl,
    framePeriod: Long
) extends ReceiverOfConnection with CoHandlerProvider {
  lazy val queue = new LinkedBlockingQueue[DictMessage]
  lazy val connectionKey = util.UUID.randomUUID.toString
  def handlers = CoHandler[LifeCycle,Unit](ConnectionRegistrationEventKey :: Nil){lifeCycle =>
    lifeCycle.onClose{()=>
      registry.store.remove(connectionKey)
      println(s"connection unregister: $connectionKey")
    }
    registry.store(connectionKey) = this
    println(s"connection   register: $connectionKey")
  } :: Nil
  def activate() = {
    val message = Option(queue.poll(framePeriod,TimeUnit.MILLISECONDS))
    if(message.nonEmpty) handlerLists.list(AlienDictMessageKey).foreach(_(message.get))
    else handlerLists.list(PeriodicMessage).foreach(_())
  }
}

class ConnectionRegistryImpl extends ConnectionRegistry {
  lazy val store = TrieMap[String, ReceiverOfConnectionImpl]()
  def send(bnd: DictMessage) = store(bnd.value("X-r-connection")).queue.add(bnd)
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