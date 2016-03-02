package ee.cone.base.server

import ee.cone.base.connection_api._

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

class KeepAlive(
  receiver: ReceiverOfConnection, sender: SenderOfConnection
) extends CoHandlerProvider {
  private var status: PingStatus = NewPingStatus
  private def command = status match {
    case NewPingStatus => "connect"
    case _: OKPingStatus => "ping"
    case WaitingPingStatus => throw new Exception("endOfLife")
  }
  private lazy val periodicFrame = new OncePer(5000, () => {
    sender.send(command,receiver.connectionKey)
    status = WaitingPingStatus
  })
  def handlers: List[BaseCoHandler] = new CoHandler[Unit,Unit] {
    def on = PeriodicMessage :: Nil
    def handle(in: Unit) = periodicFrame()
  } :: new CoHandler[DictMessage,Unit] {
    def on = AlienDictMessageKey :: Nil
    def handle(message: DictMessage) =
      message.value.get("X-r-session").foreach(sessionKey => status = OKPingStatus(sessionKey))
  } :: Nil
}


