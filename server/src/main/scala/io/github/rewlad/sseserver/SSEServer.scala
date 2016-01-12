
package io.github.rewlad.sseserver

import java.net.{ServerSocket, Socket}
import java.util.concurrent.{Executors, ScheduledExecutorService, Executor}

class SSESender(lifeTime: LifeTime, allowOriginOption: Option[String], socket: Socket)
  extends SenderOfConnection
{
  private lazy val out = lifeTime.setup(socket.getOutputStream)(_.close())
  private lazy val connected = {
    val allowOrigin =
      allowOriginOption.map(v=>s"Access-Control-Allow-Origin: $v\n").getOrElse("")
    out.write(Bytes(s"HTTP/1.1 200 OK\nContent-Type: text/event-stream\n$allowOrigin\n"))
  }
  def send(event: String, data: String) = {
    connected
    val escapedData = data.replaceAllLiterally("\n","\ndata: ")
    out.write(Bytes(s"event: $event\ndata: $escapedData\n\n"))
    out.flush()
  }
}

abstract class SSEServer {
  def pool: ScheduledExecutorService
  def allowOrigin: Option[String]
  def ssePort: Int
  def connectionRegistry: ConnectionRegistry
  def framePeriod: Long
  def purgePeriod: Long
  def createFrameHandlerOfConnection(sender: SenderOfConnection): FrameHandler

  private def createConnection(socket: Socket) = {
    val lifeTime = new LifeTime()
    val receiver = new ReceiverOfConnectionImpl(lifeTime, connectionRegistry)
    val sender = new SSESender(lifeTime, allowOrigin, socket)
    val keepAlive = new KeepAlive(receiver, sender)
    val frameHandler = createFrameHandlerOfConnection(sender)
    def handleFrame() = {
      val messageOption = receiver.poll()
      keepAlive.frame(messageOption)
      frameHandler.frame(messageOption)
    }
    val generator =
      new FrameGenerator(lifeTime, receiver, pool, framePeriod, purgePeriod, handleFrame)
    lifeTime.open()
    lifeTime.setup(socket)(_.close())
    generator.started
  }
  def start() = {
    val serverSocket = new ServerSocket(ssePort) //todo toClose
    pool.execute(ToRunnable{
      while(true) createConnection(serverSocket.accept())
    })
  }
}

abstract class SSERHttpServer extends SSEServer {
  def httpPort: Int
  def threadCount: Int

  lazy val pool = Executors.newScheduledThreadPool(threadCount)
  lazy val connectionRegistry = new ConnectionRegistry
  override def start() = {
    super.start()
    new RHttpServer {
      def httpPort = SSERHttpServer.this.httpPort
      def pool = SSERHttpServer.this.pool
      def connectionRegistry = SSERHttpServer.this.connectionRegistry
    }.start()
  }
}