package ee.cone.base.server

import java.util.UUID
import ee.cone.base.connection_api._
import ee.cone.base.util.Never

class OncePer(period: Long) {
  private var nextTime = 0L
  def apply(): Boolean = {
    val time = System.currentTimeMillis
    if(nextTime < time) {
      nextTime = time + period
      true
    } else false
  }
}

sealed trait PingStatus
case object NewPingStatus extends PingStatus
case object WaitingPingStatus extends PingStatus
case class OKPingStatus(connectionKey: String) extends PingStatus

class KeepAlive(
  handlerLists: CoHandlerLists, receiver: ReceiverOfConnection
) extends CoHandlerProvider {
  private var status: PingStatus = NewPingStatus
  private lazy val periodic = new OncePer(5000)
  private def handleMessage(message: DictMessage) =
    message.value.get("X-r-connection").foreach{ connectionKey =>
      if(connectionKey != receiver.connectionKey) Never()
      status = OKPingStatus(connectionKey)
    }
  private def periodicPingAlien() = if(periodic()){
    val command = status match {
      case NewPingStatus => "connect"
      case _: OKPingStatus => "ping"
      case WaitingPingStatus => throw new Exception("endOfLife")
    }
    status = WaitingPingStatus
    (command,receiver.connectionKey) :: Nil
  } else Nil
  def handlers: List[BaseCoHandler] =
    CoHandler(FromAlienDictMessage)(handleMessage) ::
    CoHandler(ShowToAlien)(periodicPingAlien) ::
    Nil
}


