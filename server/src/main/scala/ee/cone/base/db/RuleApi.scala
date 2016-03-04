package ee.cone.base.db

import ee.cone.base.connection_api.BaseCoHandler

trait Mandatory {
  def apply(condAttr: Attr[_], thenAttr: Attr[_], mutual: Boolean): List[BaseCoHandler]
}

trait RefIntegrity {
  def apply(existsA: Attr[Boolean], toAttr: Attr[DBNode], existsB: Attr[Boolean]): List[BaseCoHandler]
}

trait RelType {
  def apply(label: Attr[_]): List[BaseCoHandler]
}

trait Unique {
  def apply[Value](label: Attr[_], uniqueAttr: Attr[Value]): List[BaseCoHandler]
}