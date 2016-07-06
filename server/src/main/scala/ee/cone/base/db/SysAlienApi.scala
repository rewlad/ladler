package ee.cone.base.db

import java.time.Instant
import ee.cone.base.connection_api.{Attr, BaseCoHandler, Obj}

trait AlienAttributes {
  def objIdStr: Attr[String]
  def isEditing: Attr[Boolean]
  def targetObj: Attr[Obj]
  def comment: Attr[String]
  def createdAt: Attr[Option[Instant]]
}

trait Alien {
  def update[Value](attr: Attr[Value]): List[BaseCoHandler]
  def wrapForUpdate(obj: Obj): Obj
  def demanded(setup: Obj⇒Unit): Obj
}
