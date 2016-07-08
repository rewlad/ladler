package ee.cone.base.db

import ee.cone.base.connection_api._

case class ObjIdCaption(objId: ObjId) extends EventKey[String]

trait UIStrings {
  def caption(attr: Attr[_]): String
  def converter[From,To](from: AttrValueType[From], to: AttrValueType[To]): From⇒To
  def captions(label: Attr[Obj], attributes: List[Attr[_]])(calculate: Obj⇒String): List[BaseCoHandler]
}