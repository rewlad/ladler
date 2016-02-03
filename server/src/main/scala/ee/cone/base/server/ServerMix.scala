package ee.cone.base.server

import java.net.Socket
import java.nio.file.Path
import java.util.concurrent.Executors

import ee.cone.base.connection_api.ReceivedMessage

abstract class SSEHttpServer {
  def httpPort: Int
  def threadCount: Int
  def staticRoot: Path
  def allowOrigin: Option[String]
  def ssePort: Int
  def framePeriod: Long
  def purgePeriod: Long
  def createFrameHandlerOfConnection(sender: SenderOfConnection): FrameHandler

  private def createConnection(socket: Socket): Unit = {
    val lifeTime = new LifeCycleImpl()
    val receiver = new ReceiverOfConnectionImpl(lifeTime, connectionRegistry)
    val sender = new SSESender(lifeTime, allowOrigin, socket)
    val keepAlive = new KeepAlive(receiver, sender)
    val frameHandler = createFrameHandlerOfConnection(sender)
    def handleFrame() = {
      val messages = receiver.poll()
      keepAlive.frame(messages)
      frameHandler.frame(messages)
    }
    val generator =
      new FrameGenerator(lifeTime, receiver, pool, framePeriod, purgePeriod, handleFrame)
    lifeTime.open()
    lifeTime.setup(socket)(_.close())
    generator.started
  }
  lazy val pool = Executors.newScheduledThreadPool(threadCount)
  lazy val connectionRegistry = new ConnectionRegistryImpl
  def start() = {
    new RSSEServer {
      def ssePort = SSEHttpServer.this.ssePort
      def pool = SSEHttpServer.this.pool
      def createConnection(socket: Socket) = SSEHttpServer.this.createConnection(socket)
    }.start()
    new RHttpServer {
      def httpPort = SSEHttpServer.this.httpPort
      def pool = SSEHttpServer.this.pool
      def connectionRegistry = SSEHttpServer.this.connectionRegistry
      def staticRoot = SSEHttpServer.this.staticRoot
    }.start()
  }
}
