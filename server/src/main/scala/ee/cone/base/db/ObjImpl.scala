package ee.cone.base.db

import ee.cone.base.connection_api.{Attr, Obj, WrapType, CoHandlerLists}
import ee.cone.base.util.Never

class ObjImpl[WrapData](
  handlerLists: CoHandlerLists, val next: InnerObj[_],
  wrapType: WrapType[WrapData], val data: WrapData
) extends Obj with InnerObj[WrapData] {
  def apply[Value](attr: Attr[Value]) = get(this, attr)
  def update[Value](attr: Attr[Value], value: Value) = set(this, attr, value)
  def get[Value](obj: Obj, attr: Attr[Value]): Value =
    handlerLists.single(
      GetValue(wrapType, attr),
      ()⇒(obj:Obj,_:InnerObj[WrapData])⇒next.get(obj, attr)
    )(obj, this) // todo: cache it in wrapType by attr index
  def set[Value](obj: Obj, attr: Attr[Value], value: Value): Unit =
    handlerLists.single(
      SetValue(wrapType, attr),
      ()⇒(obj:Obj,_:InnerObj[WrapData], value: Value)⇒next.set(obj, attr, value)
    )(obj, this, value)
  def wrap[FWrapData](wrapType: WrapType[FWrapData], wrapData: FWrapData): Obj = {
    new ObjImpl[FWrapData](handlerLists, this, wrapType, wrapData)
  }
}
class NoObjImpl(handlerLists: CoHandlerLists) extends Obj with InnerObj[Unit] {
  def data = Never()
  def next = Never()
  def set[Value](obj: Obj, attr: Attr[Value], value: Value) = Never()
  def get[Value](obj: Obj, attr: Attr[Value]) = Never()
  def apply[Value](attr: Attr[Value]) = Never()
  def update[Value](attr: Attr[Value], value: Value) = Never()
  def wrap[FWrapData](wrapType: WrapType[FWrapData], wrapData: FWrapData) =
    new ObjImpl[FWrapData](handlerLists, this, wrapType, wrapData)
}
