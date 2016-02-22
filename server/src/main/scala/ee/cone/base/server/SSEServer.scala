
package ee.cone.base.server

import java.net.ServerSocket
import java.util.concurrent.Executor

import ee.cone.base.connection_api._
import ee.cone.base.util.{Single, ToRunnable, Bytes}

class SSESender(
  connectionLifeCycle: LifeCycle, allowOriginOption: Option[String],
  socket: SocketOfConnection
) extends SenderOfConnection {
  private lazy val out = connectionLifeCycle.setup(socket.value.getOutputStream)(_.close())
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

class RSSEServer(
  ssePort: Int, pool: Executor,
  createLifeCycle: ()=>LifeCycle,
  createConnection: (LifeCycle,List[ConnectionComponent]) ⇒ Runnable
) extends AppComponent with CanStart {
  def start() = pool.execute(ToRunnable{
    val serverSocket = new ServerSocket(ssePort) //todo toClose
    while(true) {
      val socket = serverSocket.accept()
      pool.execute(ToRunnable {
        val lifeCycle = createLifeCycle()
        lifeCycle.open()
        lifeCycle.setup(socket)(_.close())
        val connection = createConnection(lifeCycle, new SocketOfConnection(socket) :: Nil)
        connection.run()
      })
    }
  })
}
