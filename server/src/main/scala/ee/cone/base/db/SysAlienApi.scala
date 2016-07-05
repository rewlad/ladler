package ee.cone.base.db

import ee.cone.base.connection_api.{Attr, BaseCoHandler, Obj}

trait AlienAttributes {
  def objIdStr: Attr[String]
  def isEditing: Attr[Boolean]
  def targetObj: Attr[Obj]
  def comment: Attr[String]
}

trait Alien {
  def update[Value](attr: Attr[Value]): List[BaseCoHandler]
  def wrapForEdit(obj: Obj): Obj
  def demanded(setup: Obj⇒Unit): Obj
}
