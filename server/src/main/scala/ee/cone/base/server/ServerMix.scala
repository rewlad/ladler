package ee.cone.base.server


import java.nio.file.Path
import java.util.concurrent.{LinkedBlockingQueue,Executors}

import ee.cone.base.connection_api._

trait ServerAppMix extends AppMixBase {
  def httpPort: Int
  def threadCount: Int
  def staticRoot: Path
  def ssePort: Int
  def createConnection: (LifeCycle,SocketOfConnection) ⇒ Runnable

  lazy val pool = Executors.newScheduledThreadPool(threadCount)
  lazy val connectionRegistry = new ConnectionRegistryImpl
  lazy val httpServer = new RHttpServer(httpPort, staticRoot, pool, connectionRegistry)
  lazy val createLifeCycle = () => new LifeCycleImpl(None)
  lazy val sseServer = new RSSEServer(ssePort, pool, createLifeCycle, createConnection)

  override def toStart = httpServer :: sseServer :: super.toStart
}

trait ServerConnectionMix extends CoMixBase {
  def lifeCycle: LifeCycle
  def serverAppMix: ServerAppMix
  def allowOrigin: Option[String]
  def socket: SocketOfConnection
  def framePeriod: Long

  lazy val connectionRegistry = serverAppMix.connectionRegistry
  lazy val receiver = new ReceiverOfConnectionImpl(handlerLists,connectionRegistry,framePeriod)
  lazy val sender = new SSESender(lifeCycle, allowOrigin, socket)

  override def handlers =
    new KeepAlive(handlerLists,receiver, sender).handlers :::
      receiver.handlers :::
      super.handlers
}

