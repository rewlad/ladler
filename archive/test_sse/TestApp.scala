package ee.cone.base.test_sse

import java.net.Socket
import java.nio.file.{Path, Paths}

import ee.cone.base.server._



object TestApp extends App {
  val app = new TestAppMix(Nil)
  app.start()
  println(s"SEE: http://127.0.0.1:${app.httpPort}/sse.html")
}

