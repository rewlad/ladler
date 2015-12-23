package io.github.rewlad.sseserver

import java.net.InetSocketAddress
import java.nio.file.{Paths, Files}
import java.util.concurrent.Executor
import com.sun.net.httpserver.{HttpServer, HttpExchange, HttpHandler}
import scala.collection.JavaConverters.mapAsScalaMapConverter
import scala.collection.JavaConverters.iterableAsScalaIterableConverter

class StaticHandler() extends HttpHandler {
  def handle(httpExchange: HttpExchange) = Trace{ try {
    println(httpExchange.getRequestURI.getPath)
    val Mask = """(/[a-zA-Z0-9]+\.(html|js))""".r
    val Mask(fileName) = httpExchange.getRequestURI.getPath
    val bytes = Files.readAllBytes(Paths.get(s"htdocs$fileName"))
    httpExchange.sendResponseHeaders(200, bytes.length)
    httpExchange.getResponseBody.write(bytes)
  } finally httpExchange.close() }
}

class ConnectionHandler(connectionRegistry: ConnectionRegistry) extends HttpHandler {
  def handle(httpExchange: HttpExchange) = Trace{ try {
    val headers: ConnectionRegistry.Message =
      httpExchange.getRequestHeaders.asScala.mapValues(l=>Single(l.asScala.toList)).toMap
    connectionRegistry.send(headers)
    httpExchange.sendResponseHeaders(200, 0)
  } finally httpExchange.close() }
}

abstract class RHttpServer {
  def port: Int
  def pool: Executor
  def connectionRegistry: ConnectionRegistry
  def start() = {
    val server = HttpServer.create(new InetSocketAddress(port),0)
    server.setExecutor(pool)
    server.createContext("/", new StaticHandler)
    server.createContext("/connection", new ConnectionHandler(connectionRegistry))
    server.start()
  }
}
