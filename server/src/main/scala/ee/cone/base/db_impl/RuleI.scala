package ee.cone.base.db_impl

import ee.cone.base.connection_api.{Obj, Attr, BaseCoHandler}

trait Mandatory {
  def apply(condAttr: Attr[_], thenAttr: Attr[_], mutual: Boolean): List[BaseCoHandler]
}

trait RefIntegrity {
  def apply(existsA: Attr[Boolean], toAttr: Attr[Obj], existsB: Attr[Boolean]): List[BaseCoHandler]
}

trait RelType {
  def apply(label: Attr[_]): List[BaseCoHandler]
}

trait Unique {
  def apply[Value](label: Attr[Obj], uniqueAttr: Attr[Value]): List[BaseCoHandler]
}