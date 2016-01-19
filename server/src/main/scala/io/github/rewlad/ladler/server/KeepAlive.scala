package io.github.rewlad.ladler.server

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

class KeepAlive(receiver: ReceiverOfConnection, sender: SenderOfConnection) extends FrameHandler {
  var status: PingStatus = NewPingStatus
  private def command = status match {
    case NewPingStatus => "connect"
    case _: OKPingStatus => "ping"
    case WaitingPingStatus => throw new Exception("endOfLife")
  }
  private lazy val periodicFrame = new OncePer(5000, () => {
    sender.send(command,receiver.connectionKey)
    status = WaitingPingStatus
  })
  def frame(messageOption: Option[ReceivedMessage]) = {
    messageOption.foreach{ message =>
      if(ActionOf(message) == "pong")
        status = OKPingStatus(message.value("X-r-session"))
    }
    periodicFrame()
  }
}

object ActionOf {
  def apply(message: ReceivedMessage) = message.value.getOrElse("X-r-action","")
}