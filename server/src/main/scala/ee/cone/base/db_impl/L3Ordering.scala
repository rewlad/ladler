package ee.cone.base.db_impl

import ee.cone.base.connection_api._

case class OrderByAttrValueType[Value](asType: AttrValueType[Value]) extends EventKey[Option[Ordering[Value]]]

class ObjOrderingFactoryImpl(
  handlerLists: CoHandlerLists,
  attrFactory: AttrFactoryI
) extends ObjOrderingFactoryI {
  def ordering[Value](attr: Attr[Value], reverse: Boolean) = {
    val orderingForType = ordering(attrFactory.valueType(attr))
    orderingForType.map(ord⇒Ordering.by[Obj,Value](_(attr))(ord))
      .map(o⇒if(reverse) o.reverse else o)
  }
  def ordering[Value](valueType: AttrValueType[Value]) =
    handlerLists.single(OrderByAttrValueType(valueType), ()⇒None)
  def handlers[Value](asType: AttrValueType[Value])(implicit ord: Ordering[Value]) =
    List(CoHandler(OrderByAttrValueType(asType))(Some(ord)))
}
