
package io.github.rewlad.sseserver

import java.net.{ServerSocket, Socket}
import java.util
import java.util.concurrent.{ScheduledFuture, ScheduledExecutorService,
TimeUnit}

import io.github.rewlad.sseserver.ConnectionRegistry.Message

import scala.collection.concurrent.TrieMap
import scala.collection.mutable
import scala.reflect.ClassTag

////

trait Component
class Context(create: Context=>List[Component]) {
  private lazy val components = create(this)
  private lazy val byClassName = mutable.Map[String,List[Component]]() // ++ list.groupBy(_.getClass.getName)
  def list[C<:Component](implicit ct: ClassTag[C]): List[C] = {
    val cl = ct.runtimeClass
    byClassName.getOrElseUpdate(cl.getName, components.filter(cl.isInstance))
      .asInstanceOf[List[C]]
  }
  def apply[C<:Component](implicit ct: ClassTag[C]) = Single(list[C])
}

////

sealed trait LifeStatus
case object OpenableLifeStatus extends LifeStatus
class OpenLifeStatus(val toClose: List[()=>Unit]) extends LifeStatus
case object ClosingLifeStatus extends LifeStatus
class LifeTime extends Component {
  protected var status: LifeStatus = OpenableLifeStatus
  def setup[C](create: =>C)(close: C=>Unit): C = status match {
    case st: OpenLifeStatus =>
      val res = create
      status = new OpenLifeStatus((()=>close(res)) :: st.toClose)
      res
    case st => throw new Exception(s"$st")
  }
  def open() = status match {
    case OpenableLifeStatus => status = new OpenLifeStatus(Nil)
    case st => throw new Exception(s"$st")
  }
  def close() = status match {
    case st: OpenLifeStatus =>
      status = ClosingLifeStatus
      DoClose(st.toClose)
    case _ => ()
  }
}
object DoClose {
  def apply(toClose: List[()=>Unit]): Unit = toClose match {
    case Nil => ()
    case head :: tail => try head() finally apply(tail)
  }
}


////
/*
class LifeState[V](lifeTime: LifeTime, create: ()=>V, open: V=>Unit, close: V=>Unit) {
  private var state: Option[V] = None
  def apply(): V = {
    if(state.isEmpty) state = Some(setup())
    state.get
  }
  private def setup(): V = {
    lifeTime.add(() => state = None)
    val res = create()
    lifeTime.add(() => close(res))
    open(res)
    res
  }
}
object WithLifeTime {
  def apply[T](lifeTime: LifeTime)(body: =>T): T = try
    lifeTime.open()
    body
  finally lifeTime.close()
}

object ConnectionState {
  def apply[C](ctx: Context)(create: =>C)(open: C=>Unit = _=>())(close: C=>Unit = _=>()) =
    new LifeState[C](ctx[ConnectionLifeTime], ()=>create, open, close)
}
*/







class OncePer(period: Long, action: ()=>Unit) {
  private var nextTime = 0L
  def apply() = {
    val time = System.currentTimeMillis
    if(nextTime < time) {
      nextTime = time + period
      action()
    }
  }
}



trait SenderOfConnection extends Component {
  def send(event: String, data: String): Unit
}

////

trait FrameHandler extends Component {
  def frame(messageOption: Option[ConnectionRegistry.Message]): Unit
}
class ReceiverOfConnection(ctx: Context, registry: ConnectionRegistry) extends Component {
  private def createConnectionKey =
    Setup(util.UUID.randomUUID.toString)(registry.store(_)=this)
  lazy val connectionKey =
    ctx[LifeTime].setup(createConnectionKey)(registry.store.remove(_))

  private lazy val incoming = new util.ArrayDeque[ConnectionRegistry.Message]
  def poll(): Option[ConnectionRegistry.Message] =
    incoming.synchronized(Option(incoming.poll()))
  def add(message: ConnectionRegistry.Message) =
    incoming.synchronized(incoming.add(message))
}
object ConnectionRegistry {
  type Message = Map[String,String]
}
class ConnectionRegistry {
  lazy val store = TrieMap[String, ReceiverOfConnection]()
  def send(bnd: ConnectionRegistry.Message) =
    store(bnd("X-r-connection")).add(bnd)
}

/////

sealed trait PingStatus
case object NewPingStatus extends PingStatus
case object WaitingPingStatus extends PingStatus
case class OKPingStatus(sessionKey: String) extends PingStatus

class KeepAlive(ctx: Context) extends FrameHandler {
  var status: PingStatus = NewPingStatus
  private def command = status match {
    case NewPingStatus => "connect"
    case _: OKPingStatus => "ping"
    case WaitingPingStatus => throw new Exception("endOfLife")
  }
  private lazy val periodicFrame = new OncePer(5000, () => {
    ctx[SenderOfConnection].send(command,ctx[ReceiverOfConnection].connectionKey)
    status = WaitingPingStatus
  })
  def frame(messageOption: Option[Message]) = {
    messageOption.foreach{ message =>
      if(message.get("X-r-action").exists(_=="pong"))
        status = OKPingStatus(message("X-r-session"))
    }
    periodicFrame()
  }
}

/////

class FrameGenerator(
  ctx: Context,
  pool: ScheduledExecutorService,
  framePeriod: Long,
  purgePeriod: Long
) extends Component {
  private def schedule(period: Long, body: =>Unit) =
    pool.scheduleAtFixedRate(ToRunnable(Trace(body)),0,period,TimeUnit.MILLISECONDS)
  private def setup(future: ScheduledFuture[_]) =
    ctx[LifeTime].setup(future)(_.cancel(false))
  private def frameAll() = {
    val message = ctx[ReceiverOfConnection].poll()
    ctx.list[FrameHandler].foreach(_.frame(message))
  }
  private lazy val mainFuture = setup(schedule(framePeriod, frameAll()))
  private def checkCloseAll() = if(mainFuture.isDone) ctx[LifeTime].close()
  private lazy val watchFuture = setup(schedule(purgePeriod, checkCloseAll()))
  def started = (mainFuture,watchFuture)
}

/////

class SSESender(ctx: Context, allowOriginOption: Option[String], socket: Socket)
  extends SenderOfConnection
{
  private lazy val out = ctx[LifeTime].setup(socket.getOutputStream)(_.close())
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
  }
}

////

trait SSEServer {
  def sseAllowOrigin: Option[String]
  def ssePort: Int
  def sseConnectionComponents(ctx: Context): List[Component]
  def connectionRegistry: ConnectionRegistry

  def createConnection(socket: Socket) = {
    val ctx = new Context(ctx =>
      new LifeTime() :: new KeepAlive(ctx) ::
      new ReceiverOfConnection(ctx, connectionRegistry) ::
      new SSESender(ctx, sseAllowOrigin, socket) :: sseConnectionComponents(ctx)
    )
    ctx[LifeTime].open()
    ctx[LifeTime].setup(socket)(_.close())
    ctx[FrameGenerator].started
  }
  def start() = {
    val serverSocket = new ServerSocket(ssePort) //todo toClose
    println("ready")
    while(true) createConnection(serverSocket.accept())
  }
}