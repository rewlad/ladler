package ee.cone.base.server

import java.net.Socket
import java.nio.file.Path
import java.util.concurrent.{LinkedBlockingQueue,Executors}

import ee.cone.base.connection_api._

trait ServerAppMix extends MixBase[AppComponent] {
  def httpPort: Int
  def threadCount: Int
  def staticRoot: Path
  def ssePort: Int
  def createConnection: List[ConnectionComponent] ⇒ Runnable

  lazy val pool = Executors.newScheduledThreadPool(threadCount)
  lazy val connectionRegistry = new ConnectionRegistryImpl
  lazy val httpServer = new RHttpServer(httpPort, staticRoot, pool, connectionRegistry)
  lazy val sseServer = new RSSEServer(ssePort, pool, createConnection)

  override def createComponents() = httpServer :: sseServer :: super.createComponents()
}

trait ServerConnectionMix extends MixBase[ConnectionComponent] {
  def serverAppMix: ServerAppMix
  def allowOrigin: Option[String]

  lazy val connectionLifeCycle = new LifeCycleImpl(None)
  lazy val connectionRegistry = serverAppMix.connectionRegistry
  lazy val incoming = new LinkedBlockingQueue[DictMessage]
  lazy val receiver = new ReceiverOfConnectionImpl(connectionRegistry, incoming)
  lazy val sender = new SSESender(connectionLifeCycle, allowOrigin, components)
  lazy val keepAlive = new KeepAlive(receiver, sender)

  override def createComponents() =
    new ConnectionRegistration(connectionRegistry, receiver) ::
      super.createComponents()
}

