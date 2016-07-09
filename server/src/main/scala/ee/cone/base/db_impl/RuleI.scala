package ee.cone.base.db_impl

import ee.cone.base.connection_api.{Obj, Attr, BaseCoHandler}

trait RefIntegrity {
  def apply(existsA: Attr[Boolean], toAttr: Attr[Obj], existsB: Attr[Boolean]): List[BaseCoHandler]
}

trait RelType {
  def apply(label: Attr[_]): List[BaseCoHandler]
}

