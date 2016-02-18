package ee.cone.base.server

import java.net.Socket
import java.nio.file.Path
import java.util.concurrent.{LinkedBlockingQueue,Executors}

import ee.cone.base.connection_api.DictMessage

trait ServerAppMix extends MixBase[AppComponent] with CanStart {
  def httpPort: Int
  def threadCount: Int
  def staticRoot: Path
  def ssePort: Int
  def createSSEConnection: List[ConnectionComponent] ⇒ CanStart

  lazy val pool = Executors.newScheduledThreadPool(threadCount)
  lazy val connectionRegistry = new ConnectionRegistryImpl
  lazy val httpServer = new RHttpServer(httpPort, staticRoot, pool, connectionRegistry)
  lazy val sseServer = new RSSEServer(ssePort, pool, createSSEConnection)
  lazy val start = new Starter(components)

  override def createComponents() = httpServer :: sseServer :: super.createComponents()
}

trait ServerConnectionMix extends MixBase[ConnectionComponent] with CanStart {
  def serverAppMix: ServerAppMix
  def allowOrigin: Option[String]
  def purgePeriod: Long
  def runInner: ()⇒Unit

  lazy val connectionLifeCycle = new LifeCycleImpl()
  lazy val pool = serverAppMix.pool
  lazy val connectionRegistry = serverAppMix.connectionRegistry
  lazy val incoming = new LinkedBlockingQueue[DictMessage]
  lazy val receiver = new ReceiverOfConnectionImpl(connectionRegistry, connectionLifeCycle, incoming)
  lazy val sender = new SSESender(connectionLifeCycle, allowOrigin, components)
  lazy val keepAlive = new KeepAlive(receiver, sender)
  lazy val start = new ThreadPerSSEConnectionRun(pool,connectionLifeCycle, runInner)
}