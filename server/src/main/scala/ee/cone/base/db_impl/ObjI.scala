package ee.cone.base.db_impl

import ee.cone.base.connection_api.{Obj, EventKey, Attr, WrapType}

case class GetValue[WrapData,Value](wrapType: WrapType[WrapData], attr: Attr[Value])
  extends EventKey[(Obj,InnerObj[WrapData])⇒Value]
case class SetValue[WrapData,Value](wrapType: WrapType[WrapData], attr: Attr[Value])
  extends EventKey[(Obj,InnerObj[WrapData],Value)⇒Unit]
trait InnerObj[WrapData] {
  def data: WrapData
  def next: InnerObj[_]
  def get[Value](obj: Obj, attr: Attr[Value]): Value
  def set[Value](obj: Obj, attr: Attr[Value], value: Value): Unit
}
