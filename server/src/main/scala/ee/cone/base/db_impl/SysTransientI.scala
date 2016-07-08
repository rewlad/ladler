package ee.cone.base.db_impl

import ee.cone.base.connection_api.{Attr, BaseCoHandler, EventKey}

case object TransientChanged extends EventKey[()=>Unit]
trait Transient {
  def update[R](attr: Attr[R]): List[BaseCoHandler]
}
