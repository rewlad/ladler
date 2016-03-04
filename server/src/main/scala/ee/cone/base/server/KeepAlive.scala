package ee.cone.base.server

import java.util.UUID
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
    handlerLists: CoHandlerLists,
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
  def handlers: List[BaseCoHandler] =
    CoHandler[Unit,Unit](PeriodicMessage :: Nil){ in => periodicFrame() } ::
    CoHandler[DictMessage,Unit](AlienDictMessageKey :: Nil){ message =>
      message.value.get("X-r-session").foreach{ sessionKey =>
        handlerLists.list(SwitchSession).foreach(_(UUID.fromString(sessionKey)))
        status = OKPingStatus(sessionKey)
      }
    } :: Nil
}


