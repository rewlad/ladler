
package ee.cone.base.server

import java.net.{ServerSocket, Socket}
import java.util.concurrent.{Executor, ScheduledExecutorService}

import ee.cone.base.util.{Single, Setup, ToRunnable, Bytes}

class SSESender(
    connectionLifeCycle: LifeCycle, allowOriginOption: Option[String],
    components: List[ConnectionComponent]
) extends SenderOfConnection {
  private lazy val socket = Single(components.collect{ case c: SocketOfConnection ⇒
    connectionLifeCycle.setup(c.value)(_.close())
  })
  private lazy val out = connectionLifeCycle.setup(socket.getOutputStream)(_.close())
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

class RSSEServer(ssePort: Int, pool: Executor, createConnection: List[ConnectionComponent] ⇒ CanStart) extends AppComponent with CanStart {
  def start() = {
    val serverSocket = new ServerSocket(ssePort) //todo toClose
    pool.execute(ToRunnable{
      while(true)
        createConnection(new SocketOfConnection(serverSocket.accept()) :: Nil).start()
    })
  }
}
