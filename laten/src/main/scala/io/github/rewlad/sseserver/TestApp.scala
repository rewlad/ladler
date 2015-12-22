
package io.github.rewlad.sseserver

import java.net.InetSocketAddress
import java.nio.file.{Paths, Files}
import java.util.concurrent.Executors

import com.sun.net.httpserver.{HttpServer, HttpExchange, HttpHandler}
import io.github.rewlad.sseserver.ConnectionRegistry._

class StaticHandler() extends HttpHandler {
  def handle(httpExchange: HttpExchange) = Trace{ try {
    println(httpExchange.getRequestURI.getPath)
    val Mask = """/([a-z]+\.html)""".r
    val Mask(fileName) = httpExchange.getRequestURI.getPath
    val bytes = Files.readAllBytes(Paths.get(fileName))
    httpExchange.sendResponseHeaders(200, bytes.length)
    httpExchange.getResponseBody.write(bytes)
  } finally httpExchange.close() }
}

import scala.collection.JavaConverters.mapAsScalaMapConverter
import scala.collection.JavaConverters.iterableAsScalaIterableConverter

class ConnectionHandler(connectionRegistry: ConnectionRegistry) extends HttpHandler {
  def handle(httpExchange: HttpExchange) = Trace{ try {
    val headers: ConnectionRegistry.Message =
      httpExchange.getRequestHeaders.asScala.mapValues(l=>Single(l.asScala.toList)).toMap
    println(headers)
    connectionRegistry.send(headers)
    httpExchange.sendResponseHeaders(200, 0)
  } finally httpExchange.close() }
}

class MyFrameHandler(ctx: Context) extends FrameHandler {
  private var prevTime: Long = 0L
  def frame(messageOption: Option[Message]): Unit = {
    if(true){
      val time: Long = System.currentTimeMillis
      ctx[SenderOfConnection].send("show",s"$time")
    } else {
      val time: Long = System.currentTimeMillis / 100
      if(prevTime == time) return
      prevTime = time
      ctx[SenderOfConnection].send("show",s"$time")
    }
  }
}

object TestApp extends App {
  val pool = Executors.newScheduledThreadPool(5)

  val sseConnections = new ConnectionRegistry

  val server = HttpServer.create(new InetSocketAddress(5557),0)
  server.setExecutor(pool)

  server.createContext("/", new StaticHandler)
  server.createContext("/connection", new ConnectionHandler(sseConnections))
  server.start()



  new SSEServer {
    def sseAllowOrigin = Some("*")
    def ssePort = 5556
    def sseConnectionComponents(ctx: Context) =
      new FrameGenerator(ctx, pool, 20, 2000) ::
        new MyFrameHandler(ctx) :: Nil
    lazy val connectionRegistry = sseConnections
  }.start()
}

