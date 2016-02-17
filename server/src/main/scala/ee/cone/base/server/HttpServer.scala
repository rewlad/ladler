package ee.cone.base.server

import java.net.InetSocketAddress
import java.nio.file.{Path, Files}
import java.util.concurrent.Executor
import com.sun.net.httpserver.{HttpServer, HttpExchange, HttpHandler}
import ee.cone.base.connection_api.DictMessage
import ee.cone.base.util.{Single, Trace}
import scala.collection.JavaConverters.mapAsScalaMapConverter
import scala.collection.JavaConverters.iterableAsScalaIterableConverter

class StaticHandler(staticRoot: Path) extends HttpHandler {
  def handle(httpExchange: HttpExchange) = Trace{ try {
    println(httpExchange.getRequestURI.getPath)
    val Mask = """/([a-zA-Z0-9\-\.]+\.(html|js|map|png))""".r // disable ".."; add content type
    val Mask(fileName,_) = httpExchange.getRequestURI.getPath
    val bytes = Files.readAllBytes(staticRoot.resolve(fileName))
    httpExchange.sendResponseHeaders(200, bytes.length)
    httpExchange.getResponseBody.write(bytes)
  } finally httpExchange.close() }
}

class ConnectionHandler(connectionRegistry: ConnectionRegistry) extends HttpHandler {
  def handle(httpExchange: HttpExchange) = Trace{ try {
    val message =
      DictMessage(httpExchange.getRequestHeaders.asScala.mapValues(l=>Single(l.asScala.toList)).toMap)
    connectionRegistry.send(message)
    httpExchange.sendResponseHeaders(200, 0)
  } finally httpExchange.close() }
}

class RHttpServer(
  httpPort: Int,
  staticRoot: Path,
  pool: Executor,
  connectionRegistry: ConnectionRegistry
){
  def start() = {
    val server = HttpServer.create(new InetSocketAddress(httpPort),0)
    server.setExecutor(pool)
    server.createContext("/", new StaticHandler(staticRoot))
    server.createContext("/connection", new ConnectionHandler(connectionRegistry))
    server.start()
  }
}
