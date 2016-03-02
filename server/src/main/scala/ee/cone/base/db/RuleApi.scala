package ee.cone.base.db

import ee.cone.base.connection_api.BaseCoHandler

trait Mandatory {
  def apply(condAttr: Attr[_], thenAttr: Attr[_], mutual: Boolean): List[BaseCoHandler]
}
