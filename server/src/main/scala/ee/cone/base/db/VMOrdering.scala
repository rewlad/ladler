package ee.cone.base.db

import ee.cone.base.connection_api.{Attr, CoHandlerProvider, Obj}

final class CompositeOrdering[T](ordA: Ordering[T], ordB: Ordering[T]) extends Ordering[T] {
  def compare(a: T, b: T) = {
    val comp = ordA.compare(a, b)
    if(comp != 0) comp else ordB.compare(a, b)
  }
}

class ItemListOrderingAttributes(
  attr: AttrFactory,
  asBoolean: AttrValueType[Boolean],
  asObjId: AttrValueType[ObjId]
)(
  val orderByAttrValueTypeId: Attr[ObjId] = attr("83461d0d-8ec1-49a3-a4db-62def37e8a69", asObjId),
  val orderByAttrId: Attr[ObjId] = attr("064f4dfd-d3df-4748-ae5b-cc03ef42a1cb", asObjId),
  val orderDirection: Attr[Boolean] = attr("ae1bee8e-828d-42d6-a3ca-36f146549c6a", asBoolean)
)

class ItemListOrderingFactoryImpl(
  at: ItemListOrderingAttributes,
  uIStringAttributes: UIStringAttributes,
  attrFactory: AttrFactory,
  factIndex: FactIndex,
  alien: Alien,
  orderingFactory:  ObjOrderingFactory
) extends ItemListOrderingFactory with CoHandlerProvider {
  def itemList(filterObj: Obj) = {
    val orderByAttrValueTypeId = filterObj(at.orderByAttrValueTypeId)
    val orderByAttrId = filterObj(at.orderByAttrId)
    val direction = filterObj(at.orderDirection)
    new ItemListOrdering {
      def compose(defaultOrdering: Ordering[Obj]): Ordering[Obj] = {
        if(!orderByAttrId.nonEmpty){ return defaultOrdering }
        val attr = attrFactory.toAttr(orderByAttrId, AttrValueType(orderByAttrValueTypeId))
        orderingFactory.ordering(attr, direction)
          .map( o ⇒ new CompositeOrdering(o,defaultOrdering) )
          .getOrElse(defaultOrdering)
      }
      def action(attr: Attr[_]) = {
        val attrId = attrFactory.attrId(attr)
        val attrValueType = attrFactory.valueType(attr)
        val isCurrentAttrId = attrId == orderByAttrId
        val isCurrentAttrValueTypeId = attrValueType.id == orderByAttrValueTypeId
        val isCurrentAttr = isCurrentAttrId && isCurrentAttrValueTypeId
        val act = orderingFactory.ordering(attrValueType).map{ _⇒()⇒
          if(!isCurrentAttrValueTypeId) filterObj(at.orderByAttrValueTypeId) = attrValueType.id
          if(!isCurrentAttrId) filterObj(at.orderByAttrId) = attrId
          val newDir = if(isCurrentAttr) !direction else false
          if(direction != newDir) filterObj(at.orderDirection) = newDir
        }
        (act, if(isCurrentAttr) Some(direction) else None)
      }
    }
  }
  def handlers = List(
    at.orderByAttrValueTypeId, at.orderByAttrId, at.orderDirection
  ).flatMap{ attr ⇒
    factIndex.handlers(attr) ::: alien.update(attr)
  }
}
