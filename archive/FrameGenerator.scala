package ee.cone.base.server

import java.util.concurrent.{ScheduledFuture, TimeUnit,
ScheduledExecutorService}

import ee.cone.base.util.{Trace, ToRunnable}
import ee.cone.base.connection_api.Message

/*
class FrameGenerator(
  lifeTime: LifeCycle,
  //receiver: ReceiverOfConnection,
  pool: ScheduledExecutorService,
  framePeriod: Long,
  purgePeriod: Long,
  receiver: ReceiverOf[Message]
) {
  private def schedule(period: Long, body: =>Unit) =
    pool.scheduleAtFixedRate(ToRunnable(Trace(body)),0,period,TimeUnit.MILLISECONDS)
  private def setup(future: ScheduledFuture[_]) =
    lifeTime.setup(future)(_.cancel(false))
  private lazy val mainFuture = setup(schedule(framePeriod, receiver.receive(PeriodicMessage)))
  private def checkCloseAll() = if(mainFuture.isDone) lifeTime.close()
  private lazy val watchFuture = setup(schedule(purgePeriod, checkCloseAll()))
  def started = (mainFuture,watchFuture)
}
*/