
package io.github.rewlad.sseserver

import java.net.{ServerSocket, Socket}
import java.util.concurrent.Executor

class SSESender(ctx: Context, allowOriginOption: Option[String], socket: Socket)
  extends SenderOfConnection
{
  private lazy val out = ctx[LifeTime].setup(socket.getOutputStream)(_.close())
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
  def pool: Executor
  def allowOrigin: Option[String]
  def port: Int
  def connectionComponents(ctx: Context): List[Component]
  def connectionRegistry: ConnectionRegistry

  def createConnection(socket: Socket) = {
    val ctx = new Context(ctx =>
      new LifeTime() :: new KeepAlive(ctx) ::
      new ReceiverOfConnectionImpl(ctx, connectionRegistry) ::
      new SSESender(ctx, allowOrigin, socket) :: connectionComponents(ctx)
    )
    ctx[LifeTime].open()
    ctx[LifeTime].setup(socket)(_.close())
    ctx[FrameGenerator].started
  }
  def start() = {
    val serverSocket = new ServerSocket(port) //todo toClose
    pool.execute(ToRunnable{
      while(true) createConnection(serverSocket.accept())
    })
  }
}