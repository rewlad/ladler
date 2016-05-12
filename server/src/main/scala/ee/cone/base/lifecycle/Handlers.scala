package ee.cone.base.lifecycle

import ee.cone.base.connection_api._

class CoHandlerListsImpl(createHandlers: ()=>List[BaseCoHandler]) extends CoHandlerLists {
  def list[Item](ev: EventKey[Item]): List[Item] = {
    val res = value.getOrElse(ev,Nil).asInstanceOf[List[Item]]
    //println(ev, res.size)
    res
  }
  private lazy val value = createHandlers().map{ case h: CoHandler[_] ⇒ h }
    .groupBy(_.on).mapValues(_.map(_.handle))
  def single[Item](ev: EventKey[Item], fail: ()⇒Item) = list(ev) match {
    case Nil ⇒ fail()
    case h :: Nil => h
    case l => throw new Exception(s"${l.size} handlers for $ev")
  }
}
