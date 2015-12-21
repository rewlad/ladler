
package io.github.rewlad.sseserver

import java.net.{ServerSocket, Socket}
import java.util
import java.util.concurrent.{ScheduledFuture, ScheduledExecutorService,
TimeUnit}

import scala.collection.concurrent.TrieMap
import scala.collection.mutable

trait ComponentOfConnection
trait ConnectionFactory {
  protected def connectionConstructors: List[ComponentsOfConnection=>ComponentOfConnection] = Nil
  def createConnection() = new ComponentsOfConnection(connectionConstructors)
}
class ComponentsOfConnection(
  connectionConstructors: List[ComponentsOfConnection=>ComponentOfConnection]
) {
  private lazy val list: List[ComponentOfConnection] =
    connectionConstructors.map(_(this))
  private lazy val byClassName =
    mutable.Map[String,List[ComponentOfConnection]]() ++
      list.groupBy(_.getClass.getName)
  def list[C<:ComponentOfConnection](cl: Class[C]): List[C] =
    byClassName.getOrElseUpdate(cl.getName, list.filter(cl.isInstance))
      .asInstanceOf[List[C]]
  def apply[C<:ComponentOfConnection](cl: Class[C]): C = Single(list[C](cl))
}

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

trait FrameHandlerOfConnection extends ComponentOfConnection {
  def frame(): Unit
}

trait SenderOfConnection extends ComponentOfConnection {
  def send(event: String, data: String): Unit
}

/////
class CloseOfConnection extends ComponentOfConnection {
  private var closeStarted = false
  private var toClose: List[()=>Unit] = Nil
  def openClose[C](instance: =>C)(open: C=>Unit)(close: C=>Unit) = {
    if(closeStarted) throw new Exception("closing")
    val res = instance
    toClose = (()=>close(res)) :: toClose
    open(res)
    res
  }
  def close() = if(!closeStarted){
    closeStarted = true
    DoClose(toClose)
  }
}

object DoClose {
  def apply(toClose: List[()=>Unit]): Unit = toClose match {
    case Nil => ()
    case head :: tail => try head() finally apply(tail)
  }
}

/////

trait IdOfConnectionFactory extends ConnectionFactory {
  private lazy val connectionRegistry = new ConnectionRegistry
  override protected def connectionConstructors =
    (new IdOfConnection(_, connectionRegistry)) ::
    super.connectionConstructors
}

sealed trait IdStatus
case object NewIdStatus extends IdStatus
case class GeneratedIdStatus(value: String) extends IdStatus
case object ClosedIdStatus extends IdStatus

class IdOfConnection(components: ComponentsOfConnection, registry: ConnectionRegistry)
  extends ComponentOfConnection with AutoCloseable
{
  private var status: IdStatus = NewIdStatus
  def get: String = status match {
    case NewIdStatus => Setup(util.UUID.randomUUID.toString){ v =>
      status = GeneratedIdStatus(v)
    }
    case GeneratedIdStatus(v) => v
    case ClosedIdStatus => throw new Exception("closed")
  }
  def close() = status match {
    case NewIdStatus => status = ClosedIdStatus
    case GeneratedIdStatus(v) =>
      registry.remove(v)
      status = ClosedIdStatus
    case ClosedIdStatus => ()
  }
}

class ConnectionRegistry {
  lazy val data = TrieMap[String, ComponentsOfConnection]()
  def update(key: String, components: ComponentsOfConnection) =
    data(key) = components
  def remove(key: String) = data.remove(key)
}

/////

trait KeepAliveOfConnectionFactory extends IdOfConnectionFactory {
  override def connectionConstructors =
    (new KeepAliveOfConnection(_)) ::
    super.connectionConstructors
}

sealed trait PingStatus
case object NewPingStatus extends PingStatus
case object WaitingPingStatus extends PingStatus
case object OKPingStatus extends PingStatus

class KeepAliveOfConnection(components: ComponentsOfConnection) extends FrameHandlerOfConnection {
  lazy val sender = components(classOf[SenderOfConnection])
  lazy val connectionId = components(classOf[IdOfConnection]).get
  var status: PingStatus = NewPingStatus
  lazy val frame = new OncePer(5000, () => status match {
    case NewPingStatus =>
      sender.send("connect",connectionId)
      status = WaitingPingStatus
    case OKPingStatus =>
      sender.send("ping",connectionId)
      status = WaitingPingStatus
    case WaitingPingStatus => throw new Exception("endOfLife")
  })
}

/////

class FrameGeneratorOfConnection(
  components: ComponentsOfConnection,
  pool: ScheduledExecutorService,
  framePeriod: Long,
  purgePeriod: Long
) extends ComponentOfConnection with AutoCloseable {
  private lazy val mainFuture = pool.scheduleAtFixedRate(ToRunnable{
    components.list(classOf[FrameHandlerOfConnection]).foreach(_.frame())
  },0,framePeriod,TimeUnit.MILLISECONDS)
  private lazy val watchFuture: ScheduledFuture[_] = pool.scheduleAtFixedRate(ToRunnable{
    if(mainFuture.isDone) DoClose(components.list(classOf[AutoCloseable]))
  },0,purgePeriod,TimeUnit.MILLISECONDS)
  def started = watchFuture
  def close() = watchFuture.cancel(false)
}



/////

class SSESenderOfConnection(server: SSEServer, skt: Socket)
  extends SenderOfConnection with AutoCloseable
{
  private lazy val out = skt.getOutputStream
  private lazy val connected = {
    val allowOrigin =
      server.allowOrigin.map(v=>s"Access-Control-Allow-Origin: $v\n").getOrElse("")
    out.write(Bytes(s"HTTP/1.1 200 OK\nContent-Type: text/event-stream\n$allowOrigin\n"))
  }
  def send(event: String, data: String) = {
    connected
    val escapedData = data.replaceAllLiterally("\n","\ndata: ")
    out.write(Bytes(s"event: $event\ndata: $escapedData\n\n"))
    out.flush()
  }
  def close() = out.close()
}



class SSEServer(
  port: Int,
  val pool: ScheduledExecutorService,
  val allowOrigin: Option[String],
  val framePeriod: Int,
  val purgePeriod: Int
) {
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