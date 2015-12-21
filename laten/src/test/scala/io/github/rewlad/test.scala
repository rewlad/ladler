
package io.github.rewlad

import java.io.{BufferedReader, InputStreamReader}
import java.net.InetSocketAddress
import java.util.concurrent._

import com.sun.net.httpserver.{HttpExchange, HttpHandler, HttpServer}
import io.github.rewlad.sseserver.SSEServer

import org.scalastuff.json.JsonParser

class MyHandler extends HttpHandler {
  def handle(t: HttpExchange) = {
    val bufferedReader = new BufferedReader(new InputStreamReader(t.getRequestBody,"UTF-8"))
    val jsonHandler = new TestJsonHandler
    val parser = new JsonParser(jsonHandler)
    parser.parse(bufferedReader)
    val jreq :: Nil = jsonHandler.state


    val bytes = "[]".getBytes("UTF-8")
    t.sendResponseHeaders(200, bytes.length)
    val os = t.getResponseBody
    os.write(bytes)
    os.close()
  }
}



object Test0 extends App {
  val pool = Executors.newScheduledThreadPool(5)

  val server = HttpServer.create(new InetSocketAddress(5557),0)
  server.setExecutor(pool)
  server.createContext("/", new MyHandler())
  server.start()

  new SSEServer(5556,pool,Some("*"),20).start()

}
