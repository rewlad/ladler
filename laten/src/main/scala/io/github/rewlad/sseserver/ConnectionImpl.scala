package io.github.rewlad.sseserver

import java.util
import java.util.concurrent.{ScheduledFuture, TimeUnit, ScheduledExecutorService}
import scala.collection.concurrent.TrieMap

class ReceiverOfConnectionImpl(ctx: Context, registry: ConnectionRegistry) extends ReceiverOfConnection {
  private def createConnectionKey =
    Setup(util.UUID.randomUUID.toString)(registry.store(_)=this)
  lazy val connectionKey =
    ctx[LifeTime].setup(createConnectionKey)(registry.store.remove(_))

  private lazy val incoming = new util.ArrayDeque[ReceiverOfConnection.Message]
  var messageOption: Option[ReceiverOfConnection.Message] = None
  def poll() = incoming.synchronized(messageOption = Option(incoming.poll()))
  def add(message: ReceiverOfConnection.Message) =
    incoming.synchronized(incoming.add(message))
}

class ConnectionRegistry {
  lazy val store = TrieMap[String, ReceiverOfConnectionImpl]()
  def send(bnd: ReceiverOfConnection.Message) =
    store(bnd("X-r-connection")).add(bnd)
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
    ctx[ReceiverOfConnection].poll()
    ctx.list[FrameHandler].foreach(_.frame())
  }
  private lazy val mainFuture = setup(schedule(framePeriod, frameAll()))
  private def checkCloseAll() = if(mainFuture.isDone) ctx[LifeTime].close()
  private lazy val watchFuture = setup(schedule(purgePeriod, checkCloseAll()))
  def started = (mainFuture,watchFuture)
}

