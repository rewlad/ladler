package io.github.rewlad.sseserver

import java.util
import java.util.concurrent.{ScheduledFuture, TimeUnit, ScheduledExecutorService}
import scala.collection.concurrent.TrieMap

class ReceiverOfConnectionImpl(lifeTime: LifeTime, registry: ConnectionRegistry) extends ReceiverOfConnection {
  private def createConnectionKey = Setup(util.UUID.randomUUID.toString){ k =>
    registry.store(k) = this
    println(s"connection   register: $k")
  }
  lazy val connectionKey = lifeTime.setup(createConnectionKey){ k =>
    registry.store.remove(k)
    println(s"connection unregister: $k")
  }

  private lazy val incoming = new util.ArrayDeque[ReceivedMessage]
  def poll(): Option[ReceivedMessage] =
    incoming.synchronized(Option(incoming.poll()))
  def add(message: ReceivedMessage) =
    incoming.synchronized(incoming.add(message))
}

class ConnectionRegistry {
  lazy val store = TrieMap[String, ReceiverOfConnectionImpl]()
  def send(bnd: ReceivedMessage) =
    store(bnd.value("X-r-connection")).add(bnd)
}

/////

class FrameGenerator(
  lifeTime: LifeTime,
  receiver: ReceiverOfConnection,
  pool: ScheduledExecutorService,
  framePeriod: Long,
  purgePeriod: Long,
  handleFrame: ()=>Unit
) {
  private def schedule(period: Long, body: =>Unit) =
    pool.scheduleAtFixedRate(ToRunnable(Trace(body)),0,period,TimeUnit.MILLISECONDS)
  private def setup(future: ScheduledFuture[_]) =
    lifeTime.setup(future)(_.cancel(false))
  private lazy val mainFuture = setup(schedule(framePeriod, handleFrame()))
  private def checkCloseAll() = if(mainFuture.isDone) lifeTime.close()
  private lazy val watchFuture = setup(schedule(purgePeriod, checkCloseAll()))
  def started = (mainFuture,watchFuture)
}

