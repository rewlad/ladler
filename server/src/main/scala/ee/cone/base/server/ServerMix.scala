package ee.cone.base.server


import java.nio.file.Path
import java.util.concurrent.{LinkedBlockingQueue,Executors}

import ee.cone.base.connection_api._

trait ServerAppMix extends AppMixBase {
  def httpPort: Int
  def threadCount: Int
  def staticRoot: Path
  def ssePort: Int
  def createAlienConnection: LifeCycle ⇒ CoMixBase

  lazy val connectionRegistry = new ConnectionRegistryImpl
  lazy val httpServer = new RHttpServer(httpPort, staticRoot, executionManager, connectionRegistry)

  lazy val sseServer = new RSSEServer(ssePort, executionManager, createAlienConnection)

  override def toStart = httpServer :: sseServer :: super.toStart
}

trait ServerConnectionMix extends CoMixBase {
  def lifeCycle: LifeCycle
  def serverAppMix: ServerAppMix
  def allowOrigin: Option[String]
  def framePeriod: Long

  lazy val connectionRegistry = serverAppMix.connectionRegistry
  lazy val receiver = new ReceiverOfConnectionImpl(lifeCycle,handlerLists,connectionRegistry,framePeriod)
  lazy val sender = new SSESender(allowOrigin)

  override def handlers =
    new KeepAlive(handlerLists,receiver, sender).handlers :::
      receiver.handlers :::
      sender.handlers :::
      super.handlers
}

