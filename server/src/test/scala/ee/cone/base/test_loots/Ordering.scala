package ee.cone.base.test_loots

import ee.cone.base.connection_api._
import ee.cone.base.db._

case class OrderByAttr(attrId: ObjId) extends EventKey[Option[Ordering[Obj]]]

final class CompositeOrdering[T](ordA: Ordering[T], ordB: Ordering[T]) extends Ordering[T] {
  def compare(a: T, b: T) = {
    val comp = ordA.compare(a, b)
    if(comp != 0) comp else ordB.compare(a, b)
  }
}

trait ItemListOrdering {
  def compose(defaultOrdering: Ordering[Obj]): Ordering[Obj]
  def action(attr: Attr[_]): (Option[()⇒Unit],Option[Boolean])
}

class ItemListOrderingAttributes(
  attr: AttrFactory,
  asBoolean: AttrValueType[Boolean],
  asObjId: AttrValueType[ObjId]
)(
  val orderByAttrId: Attr[ObjId] = attr("064f4dfd-d3df-4748-ae5b-cc03ef42a1cb", asObjId),
  val orderDirection: Attr[Boolean] = attr("ae1bee8e-828d-42d6-a3ca-36f146549c6a",asBoolean)
)

class ItemListOrderingFactory(
  at: ItemListOrderingAttributes,
  handlerLists: CoHandlerLists,
  attrFactory: AttrFactory,
  factIndex: FactIndex,
  alien: Alien
) extends CoHandlerProvider {
  def itemList(filterObj: Obj): ItemListOrdering = {
    val orderByAttrId = filterObj(at.orderByAttrId)
    val direction = filterObj(at.orderDirection)
    new ItemListOrdering {
      def compose(defaultOrdering: Ordering[Obj]): Ordering[Obj] = {
        ordering(orderByAttrId, direction)
          .map( o ⇒ new CompositeOrdering(o,defaultOrdering) )
          .getOrElse(defaultOrdering)
      }
      def action(attr: Attr[_]) = {
        val attrId = attrFactory.attrId(attr)
        val isCurrentAttr = attrId == orderByAttrId
        val act = if(ordering(attrId, reverse=false).isEmpty) None else Some{ ()⇒
          if(!isCurrentAttr) filterObj(at.orderByAttrId) = attrId
          val newDir = if(isCurrentAttr) !direction else false
          if(direction != newDir) filterObj(at.orderDirection) = newDir
        }
        (act, if(isCurrentAttr) Some(direction) else None)
      }
    }
  }
  def ordering(attrId: ObjId, reverse: Boolean): Option[Ordering[Obj]] =
    handlerLists.single(OrderByAttr(attrId), ()⇒None).map( o ⇒ if(reverse) o.reverse else o )
  def handlers[T](attr: Attr[T])(implicit ord: Ordering[T]): List[BaseCoHandler] =
    List(CoHandler(OrderByAttr(attrFactory.attrId(attr)))(Some(Ordering.by(_(attr)))))
  def handlers = List(
    at.orderByAttrId, at.orderDirection
  ).flatMap{ attr⇒
    factIndex.handlers(attr) ::: alien.update(attr)
  }
}
