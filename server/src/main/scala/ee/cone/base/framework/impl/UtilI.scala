
package ee.cone.base.framework

import ee.cone.base.connection_api.Obj

// to app

trait Measure {
  def apply[T](activity: ()=>T)(handlePeriod: (Long,Long)⇒Unit): T
}
