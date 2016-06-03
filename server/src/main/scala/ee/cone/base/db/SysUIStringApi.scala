package ee.cone.base.db

import ee.cone.base.connection_api.{Attr, BaseCoHandler, EventKey, Obj}

case class AttrCaption(attr: Attr[_]) extends EventKey[String]

case class ToUIStringConverter[Value](valueType: AttrValueType[Value])
  extends EventKey[Value⇒String]

trait UIStrings {
  def caption(attr: Attr[_]): String
  def convert[Value](value: Value, valueType: AttrValueType[Value]): String
  def fromString[Value](value: String, valueType: AttrValueType[Value]): Value
  def handlers(attributes: List[Attr[_]])(calculate: Obj⇒String): List[BaseCoHandler]
}