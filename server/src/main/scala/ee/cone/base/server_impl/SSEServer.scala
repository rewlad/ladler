
package ee.cone.base.server_impl

import java.io.OutputStream
import java.net.ServerSocket

import ee.cone.base.connection_api._
import ee.cone.base.util.Bytes

class SSESender(
  allowOriginOption: Option[String]
) extends SenderOfConnection with CoHandlerProvider {
  private var outOpt: Option[OutputStream] = None
  private def out: OutputStream = outOpt.get
  def handlers = CoHandler(SetOutput){ out => outOpt = Option(out) } :: Nil
  private lazy val connected = {
    val allowOrigin =
      allowOriginOption.map(v=>s"Access-Control-Allow-Origin: $v\n").getOrElse("")
    out.write(Bytes(s"HTTP/1.1 200 OK\nContent-Type: text/event-stream\n$allowOrigin\n"))
  }
  def sendToAlien(event: String, data: String) = {
    connected
    val escapedData = data.replaceAllLiterally("\n","\ndata: ")
    out.write(Bytes(s"event: $event\ndata: $escapedData\n\n"))
    out.flush()
    println(s"event: $event\ndata: $escapedData\n\n")
  }
}

class RSSEServer(
  ssePort: Int,
  lifeCycleManager: ExecutionManager,
  createConnection: LifeCycle ⇒ CoMixBase
) extends CanStart {
  private lazy val serverSocket = new ServerSocket(ssePort) //todo toClose
  def start() = lifeCycleManager.submit{ ()=>
    while(true){
      val socket = serverSocket.accept()
      lifeCycleManager.startConnection{ lifeCycle =>
        lifeCycle.onClose(()=>socket.close())
        val out = socket.getOutputStream
        lifeCycle.onClose(()=>out.close())

        val connection = createConnection(lifeCycle)
        connection.handlerLists.list(SetOutput).foreach(_(out))
        connection
      }
    }
  }
}
