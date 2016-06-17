package ee.cone.base.db

import ee.cone.base.connection_api.{Attr, BaseCoHandler, EventKey, Obj}

case class AttrCaption(attr: Attr[_]) extends EventKey[String]
case class ObjIdCaption(objId: ObjId) extends EventKey[String]

case class ToUIStringConverter[From,To](from: AttrValueType[From], to: AttrValueType[To])
  extends EventKey[From⇒To]

trait UIStrings {
  def caption(attr: Attr[_]): String
  def converter[From,To](from: AttrValueType[From], to: AttrValueType[To]): From⇒To
  def captions(label: Attr[Obj], attributes: List[Attr[_]])(calculate: Obj⇒String): List[BaseCoHandler]
}