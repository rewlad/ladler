package ee.cone.base.lifecycle

import ee.cone.base.connection_api._

class CoHandlerListsImpl(createHandlers: ()=>List[BaseCoHandler]) extends CoHandlerLists {
  def list[In,Out](ev: EventKey[In,Out]): List[In=>Out] = {
    val res = value.getOrElse(ev,Nil).asInstanceOf[List[In=>Out]]
    println(ev, res.size)
    res
  }
  private lazy val value = createHandlers().map{ case h: CoHandler[_,_] ⇒ h }
    .groupBy(_.on).mapValues(_.map(_.handle))
  def single[In, Out](ev: EventKey[In, Out]) = list(ev) match {
    case h :: Nil => h
    case l => throw new Exception(s"${l.size} handlers for $ev")
  }
}
