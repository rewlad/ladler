package ee.cone.base.lifecycle

import ee.cone.base.connection_api._

class CoHandlerListsImpl(createHandlers: ()=>List[BaseCoHandler]) extends CoHandlerLists {
  def list[In,Out](ev: EventKey[In,Out]): List[In=>Out] =
    value.getOrElse(ev,Nil).asInstanceOf[List[In=>Out]]
  private lazy val value = createHandlers().map{ case h: CoHandler[_,_] ⇒ h }
    .groupBy(_.on).mapValues(_.map(_.handle))
}
