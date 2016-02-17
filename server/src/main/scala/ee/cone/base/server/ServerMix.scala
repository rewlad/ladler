package ee.cone.base.server

import java.net.Socket
import java.nio.file.Path
import java.util.concurrent.{Executor, LinkedBlockingQueue, BlockingQueue,
Executors}

import ee.cone.base.connection_api.{DictMessage, Message}
import ee.cone.base.util.ToRunnable

trait ServerAppMix {
  def httpPort: Int
  def threadCount: Int
  def staticRoot: Path
  def ssePort: Int

  lazy val pool = Executors.newScheduledThreadPool(threadCount)
  lazy val connectionRegistry = new ConnectionRegistryImpl
  lazy val httpServer = new RHttpServer(httpPort, staticRoot, pool, connectionRegistry)
  lazy val sseServer = new RSSEServer(ssePort, pool, )
  lazy val connectionMana

  trait ServerConnectionMix {
    def connectionLifeCycle: LifeCycle
    def socket: Socket
    def allowOrigin: Option[String]
    def purgePeriod: Long

    lazy val incoming = new LinkedBlockingQueue[DictMessage]
    lazy val receiver = connectionRegistry.createReceiver(connectionLifeCycle, incoming)
    lazy val sender = new SSESender(connectionLifeCycle, allowOrigin, socket)
    lazy val keepAlive = new KeepAlive(receiver, sender)
  }
}


class ContextOfConnection(val sender: SenderOfConnection, val lifeCycle: LifeCycle)

abstract class SSEHttpServer {

  def createMessageReceiverOfConnection(context: ContextOfConnection): ReceiverOf[Message]

  protected def createConnection(connectionLifeCycle: LifeCycle, socket: Socket): Unit


  def start() = {
    new RSSEServer {
      def ssePort = SSEHttpServer.this.ssePort
      def pool = SSEHttpServer.this.pool
      def createConnection(socket: Socket) = {

      }
    }.start()

  }
}

trait ConnectionManager {
  def createConnection(socket: Socket): Unit
}

class ThreadPerConnectionManager(
  pool: Executor, runConnection: (LifeCycle,Socket)=>Unit
) extends ConnectionManager {
  def createConnection(socket: Socket) = pool.execute(ToRunnable{
    val connectionLifeCycle = new LifeCycleImpl()
    try{
      connectionLifeCycle.open()
      connectionLifeCycle.setup(socket)(_.close())
      runConnection(connectionLifeCycle, socket)
    } finally connectionLifeCycle.close()
  })
}