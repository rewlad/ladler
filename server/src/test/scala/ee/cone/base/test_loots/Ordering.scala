package ee.cone.base.test_loots

import java.time.{LocalTime, Instant}
import javax.xml.datatype.Duration
import ee.cone.base.connection_api._
import ee.cone.base.db._

//case class OrderByAttr(attrId: ObjId) extends EventKey[Option[Ordering[Obj]]]

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
  val orderByAttrValueTypeId: Attr[ObjId] = attr("83461d0d-8ec1-49a3-a4db-62def37e8a69", asObjId),
  val orderByAttrId: Attr[ObjId] = attr("064f4dfd-d3df-4748-ae5b-cc03ef42a1cb", asObjId),
  val orderDirection: Attr[Boolean] = attr("ae1bee8e-828d-42d6-a3ca-36f146549c6a", asBoolean)
)

class ItemListOrderingFactory(
  at: ItemListOrderingAttributes,
  attrFactory: AttrFactory,
  factIndex: FactIndex,
  alien: Alien,
  orderingFactory:  ObjOrderingFactory
) extends CoHandlerProvider {
  def itemList(filterObj: Obj): ItemListOrdering = {
    val orderByAttrValueTypeId = filterObj(at.orderByAttrValueTypeId)
    val orderByAttrId = filterObj(at.orderByAttrId)
    val direction = filterObj(at.orderDirection)
    new ItemListOrdering {
      def compose(defaultOrdering: Ordering[Obj]): Ordering[Obj] = {
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

////

class ObjOrderingForAttrValueTypes(
    objOrderingFactory: ObjOrderingFactory,
    asBoolean: AttrValueType[Boolean],
    asString: AttrValueType[String],
    asObj: AttrValueType[Obj],
    asInstant: AttrValueType[Option[Instant]],
    asLocalTime: AttrValueType[Option[LocalTime]],
    uiStrings: UIStrings
) extends CoHandlerProvider {
  def handlers =
    objOrderingFactory.handlers(asBoolean) :::
    objOrderingFactory.handlers(asString) :::
    objOrderingFactory.handlers(asObj)(Ordering.by(obj⇒uiStrings.converter(asObj,asString)(obj))) :::
    objOrderingFactory.handlers(asInstant) :::
    objOrderingFactory.handlers(asLocalTime)
}

////

case class OrderByAttrValueType[Value](asType: AttrValueType[Value]) extends EventKey[Option[Ordering[Value]]]

class ObjOrderingFactory(
    handlerLists: CoHandlerLists,
    attrFactory: AttrFactory
) {
  def ordering[Value](attr: Attr[Value], reverse: Boolean): Option[Ordering[Obj]] =
    ordering(attrFactory.valueType(attr))
      .map(ord⇒Ordering.by[Obj,Value](_(attr))(ord))
  def ordering[Value](valueType: AttrValueType[Value]): Option[Ordering[Value]] =
    handlerLists.single(OrderByAttrValueType(valueType), ()⇒None)
  def handlers[Value](asType: AttrValueType[Value])(implicit ord: Ordering[Value]): List[BaseCoHandler] =
    List(CoHandler(OrderByAttrValueType(asType))(Some(ord)))
}