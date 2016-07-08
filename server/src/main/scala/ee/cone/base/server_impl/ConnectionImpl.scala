package ee.cone.base.server_impl

import java.util
import java.util.concurrent.{TimeUnit, LinkedBlockingQueue, BlockingQueue}

import ee.cone.base.connection_api._
import scala.collection.concurrent.TrieMap

class ReceiverOfConnectionImpl(
  lifeCycle: LifeCycle,
  handlerLists: CoHandlerLists,
  registry: ConnectionRegistryImpl,
  framePeriod: Long,
  sender: SenderOfConnection
) extends ReceiverOfConnection with CoHandlerProvider {
  lazy val queue = new LinkedBlockingQueue[DictMessage]
  lazy val connectionKey = {
    val key = util.UUID.randomUUID.toString
    lifeCycle.onClose{()=>
      registry.store.remove(key)
      println(s"connection unregister: $key")
    }
    registry.store(key) = this
    println(s"connection   register: $key")
    key
  }
  def handlers = CoHandler(ActivateReceiver){ ()=>
    Option(queue.poll(framePeriod,TimeUnit.MILLISECONDS)).foreach(message=>
      handlerLists.list(FromAlienDictMessage).foreach(_(message))
    )
    handlerLists.list(ShowToAlien).flatMap(_()).foreach{
      case (command,content) => sender.sendToAlien(command,content)
    }
  } :: Nil
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