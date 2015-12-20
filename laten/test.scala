import java.io.{BufferedReader, InputStreamReader}
import java.net.{ServerSocket, InetSocketAddress, Socket}
import java.sql.Connection
import java.util
import java.util.concurrent._
import java.util.concurrent.atomic.AtomicInteger
import com.sun.net.httpserver.{HttpExchange, HttpHandler, HttpServer}

import org.scalastuff.json.{JsonParser,JsonHandler}
import scala.annotation.tailrec

/*
case object StartJSO

class TestJsonHandler extends JsonHandler {
  trait Ctx {
    def parent: Ctx
    def result: Object
    def add(v: Object): Unit
    def key_= (k: String): Unit
    def end() = { parent.add(result); parent }
  }
  class ArrayCtx(val parent: Ctx) extends Ctx {
    private var acc: List[Object] = Nil
    def result = acc.reverse
    def add(v: Object) = acc = v :: acc
    def key_= (k: String) = ???
  }
  class ObjectCtx(val parent: Ctx) extends Ctx {
    var result: Map[String,Object] = Map()
    var key = ""
    def add(v: Object) = result += key → v
  }
  object RootCtx extends Ctx {
    def parent = ???


  }

  var ctx: Ctx = RootCtx

  def startObject() = ctx = new ObjectCtx(ctx)
  def endObject()   = ctx = ctx.end()
  def startArray()  = ctx = new ArrayCtx(ctx)
  def endArray()    = ctx = ctx.end()

  def string(s: String): Unit = ctx.add(s)
  def number(n: String): Unit = ctx.add(BigDecimal(n))
  def falseValue(): Unit = ???
  def trueValue(): Unit = ???
  def nullValue(): Unit = ???

  def startMember(name: String) = ctx.key = name
  def error(message: String, line: Int, pos: Int, excerpt: String): Unit = ???
}
*/

class TestJsonHandler extends JsonHandler {
  object StartJSO
  var state: List[Object]  = Nil

  @tailrec private def endObject(r: Map[String,Object], l: List[Object]): List[Object] = l match {
    case h :: t if h eq StartJSO ⇒ r :: t
    case v :: k :: t ⇒ endObject(r+(k.asInstanceOf[String]→v), t)
  }
  @tailrec private def endArray(r: List[Object], l: List[Object]): List[Object] = l match {
    case h :: t if h eq StartJSO ⇒ r :: t
    case v :: t ⇒ endArray(v :: r, t)
  }

  def startObject() = state = StartJSO :: state
  def endObject()   = state = endObject(Map(),state)
  def startArray()  = state = StartJSO :: state
  def endArray()    = state = endArray(Nil,state)

  def string(s: String) = state = s :: state
  def number(n: String) = state = BigDecimal(n) :: state
  def falseValue(): Unit = ???
  def trueValue(): Unit = ???
  def nullValue(): Unit = ???

  def startMember(name: String) = state = name :: state
  def error(message: String, line: Int, pos: Int, excerpt: String): Unit = ???
}

case class Point(x: Int, y: Int){
  def -(that: Point) = Point(x-that.x,y-that.y)
  def +(that: Point) = Point(x+that.x,y+that.y)
  def r2 = x*x+y*y
}
case class Circle(center: Point, r: Int)
case class Hold(circle: Circle, cursor: Point){
  def move(p: Point): Hold = copy(
    circle = circle.copy(center=circle.center-cursor+p),
    cursor = p
  )
}
case class World(version: Int=0, hold: Option[Hold]=None, passiveCircles: List[Circle]=Nil){
  def r = 20
  def withManyCircles = copy(passiveCircles = (for(x←0 to 15; y←0 to 15) yield Circle(Point((1+x*2)*r,(1+y*2)*r),r)).toList)
  def beginHold(cursor: Point): World = if(hold.isEmpty){
    passiveCircles.find{ c ⇒ (c.center - cursor).r2 < c.r*c.r }.map{ circle ⇒
      copy(
        version = version + 1,
        passiveCircles = passiveCircles.filter(circle ne _),
        hold = Some(Hold(circle, cursor))
      )
    }.getOrElse(this)
  } else this
  def moveHold(cursor: Point): World = hold.map{ hold ⇒
    copy(
      version = version + 1,
      hold = Some(hold.move(cursor))
    )
  }.getOrElse(this)
  def endHold: World = hold.map{ hold ⇒
    copy(
      version = version + 1,
      passiveCircles = hold.circle :: passiveCircles,
      hold = None
    )
  }.getOrElse(this)
}


class Desktop {
  def connectionId: String = ???
  var worldState: World = World().withManyCircles


}




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

object ToRunnable {
  def apply(f: => Unit) = new Runnable() { def run() { f } }
}

class SerialExecutor(executor: Executor) {
  private lazy val tasks = new util.ArrayDeque[Runnable]
  private var active: Option[Runnable] = None
  def apply(f: ⇒Unit) = synchronized {
    tasks.offer(ToRunnable{ try f finally scheduleNext() })
    if(active.isEmpty) scheduleNext()
  }
  private def scheduleNext(): Unit = synchronized {
    active = Option(tasks.poll())
    active.foreach(executor.execute)
  }
}

import java.nio.charset.StandardCharsets.UTF_8
object Bytes {
  def apply(content: String) = content.getBytes(UTF_8)
}

class SSEConnection(server: SSEServer, skt: Socket) {
  private lazy val out = skt.getOutputStream
  lazy val connectionId = util.UUID.randomUUID.toString
  private def send(event: String, data: String) = {
    val pdata = data.replaceAllLiterally("\n","\ndata: ")
    out.write(Bytes(s"event: $event\ndata: $data\n\n"))
    out.flush()
  }
  lazy val scheduled =
    server.pool.scheduleAtFixedRate(ToRunnable(frame()),0,server.framePeriod,TimeUnit.MILLISECONDS)
  private lazy val connected = {
    val allowOrigin =
      server.allowOrigin.map(v=>s"Access-Control-Allow-Origin: $v\n").getOrElse("")
    out.write(Bytes(s"HTTP/1.1 200 OK\nContent-Type: text/event-stream\n$allowOrigin\n"))
    send("connect",connectionId)
    // prolongLife
  }
  private var endOfLife: Long = 0
  private def frame() = {
    connected
    if(System.currentTimeMillis() < endOfLife) throw new Exception("endOfLife")
    // ping
    ???
  }
  def close() = out.close()
  def ping() = {

    //if(System.currentTimeMillis() < endOfLife.get) send("ping")
  }
}



class SSEServer(port: Int, val pool: ScheduledExecutorService, val allowOrigin: Option[String], val framePeriod: Int) {
  private var connectionById = Map[String,SSEConnection]()
  private def register(connection: SSEConnection) = synchronized{
    connectionById = connectionById + (connection.connectionId → connection)
  }
  private def unregister(connection: SSEConnection) = synchronized {
    connectionById = connectionById - connection.connectionId
  }
  private def purge() = synchronized {
    connectionById.valuesIterator.foreach{ connection =>
      if(connection.scheduled.isDone){
        unregister(connection)
        connection.close()
      }
    }
  }

  def start() = {
    val serverSocket = new ServerSocket(port) //todo toClose
    pool.scheduleAtFixedRate(ToRunnable(purge()),0,5,TimeUnit.SECONDS) //todo toClose, trace?
    println("ready")
    while(true){
      val skt = serverSocket.accept()
      println("connected")
      val connection = new SSEConnection(this, skt)
      register(connection)
      connection.scheduled
    }
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


object Test1 extends App {
  val pool = Executors.newScheduledThreadPool(5)
  val aCount = new AtomicInteger(0)
  val bCount = new AtomicInteger(0)
  val aFuture: ScheduledFuture[_] = pool.scheduleAtFixedRate(ToRunnable{
    println(s"a:${aCount.incrementAndGet()}")
    if(aCount.get==3){ // skipped iterations
      Thread.sleep(3000)
      println(s"a:exit")
    }
    if(aCount.get==7){
      aFuture.cancel(false) // true'll cause interrupt during the sleep
      Thread.sleep(1)
      println(s"a:cancel")
    }
  },100,1000,TimeUnit.MILLISECONDS)
  pool.scheduleAtFixedRate(ToRunnable{
    println(s"b:${bCount.incrementAndGet()}")
    if(bCount.get==8){
      throw new Exception("b failed") // will not run next time
    }
  },200,1000,TimeUnit.MILLISECONDS)
}

object Test2 extends App {
  val pool = Executors.newScheduledThreadPool(5)
  val aCount = new AtomicInteger(0)
  val aFuture: ScheduledFuture[_] = pool.scheduleAtFixedRate(ToRunnable{
    println(s"a:${aCount.incrementAndGet()}")
    if(aCount.get==3) throw new Exception("b failed") // aFuture.isDone 'll be true after that
  },100,1000,TimeUnit.MILLISECONDS)
  pool.scheduleAtFixedRate(ToRunnable{
    println(s"${aFuture.isCancelled}:${aFuture.isDone}")
  },200,1000,TimeUnit.MILLISECONDS)
}