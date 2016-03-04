
package ee.cone.base.server

import java.io.OutputStream
import java.net.{Socket, ServerSocket}
import java.util.concurrent.Executor

import ee.cone.base.connection_api._
import ee.cone.base.util.{Single, ToRunnable, Bytes}

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
  createConnection: LifeCycle ⇒ CoMixBase
) extends CanStart {
  def start() = pool.execute(ToRunnable{
    val serverSocket = new ServerSocket(ssePort) //todo toClose
    while(true) {
      val socket = serverSocket.accept()
      pool.execute(ToRunnable {
        val lifeCycle = createLifeCycle()
        try{
          lifeCycle.open()
          lifeCycle.onClose(()=>socket.close())
          val out = socket.getOutputStream
          lifeCycle.onClose(()=>out.close())
          val connection = createConnection(lifeCycle)
          try{
            connection.handlerLists.list(SetOutput).foreach(_(out))
            while(true) Single(connection.handlerLists.list(ActivateReceiver))()
          } catch {
            case e: Exception ⇒
              connection.handlerLists.list(FailEventKey).foreach(_(e))
              throw e
          }
        } finally lifeCycle.close()
      })
    }
  })
}

