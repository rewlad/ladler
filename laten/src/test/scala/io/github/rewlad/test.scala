
package io.github.rewlad

import java.io.{BufferedReader, InputStreamReader}
import java.net.InetSocketAddress
import java.nio.file.{Files, Paths}
import java.util
import java.util.concurrent._

import com.sun.net.httpserver.{Headers, HttpExchange, HttpHandler, HttpServer}
import io.github.rewlad.sseserver.ConnectionRegistry.Message
import io.github.rewlad.sseserver._

import org.scalastuff.json.JsonParser

import scala.collection.{Map, mutable}

/*
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
*/



