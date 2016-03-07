package ee.cone.base.test_react_db

import ee.cone.base.connection_api._
import ee.cone.base.vdom.ViewPath
import ee.cone.base.db.SessionEventSourceOperations

class FailOfConnection(
  sender: SenderOfConnection
) extends CoHandlerProvider {
  def handlers = CoHandler(FailEventKey){ e =>
    sender.sendToAlien("fail",e.toString) //todo
  } :: Nil
}

class DynEdit(
  eventSourceOperations: SessionEventSourceOperations
) extends CoHandlerProvider {
  //lazy val
  def handlers = CoHandler(ViewPath("/db")){ pf =>
    // until = ???
    eventSourceOperations.incrementalApplyAndView{ ()⇒
      ???
    }
  } :: Nil
}
