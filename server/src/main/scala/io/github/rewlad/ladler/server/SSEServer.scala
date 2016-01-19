
package io.github.rewlad.ladler.server

import java.net.{ServerSocket, Socket}
import java.util.concurrent.ScheduledExecutorService

import io.github.rewlad.ladler.connection_api.SenderOfConnection
import io.github.rewlad.ladler.util.{ToRunnable, Bytes}

class SSESender(lifeTime: LifeCycle, allowOriginOption: Option[String], socket: Socket)
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
    print(escapedData)
  }
}

abstract class RSSEServer {
  def pool: ScheduledExecutorService
  def createConnection(socket: Socket)
  def ssePort: Int
  def start() = {
    val serverSocket = new ServerSocket(ssePort) //todo toClose
    pool.execute(ToRunnable{
      while(true) createConnection(serverSocket.accept())
    })
  }
}
