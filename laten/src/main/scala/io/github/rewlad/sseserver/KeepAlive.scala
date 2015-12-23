package io.github.rewlad.sseserver

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
  def frame(messageOption: Option[ConnectionRegistry.Message]) = {
    messageOption.foreach{ message =>
      if(message.get("X-r-action").exists(_=="pong"))
        status = OKPingStatus(message("X-r-session"))
    }
    periodicFrame()
  }
}
