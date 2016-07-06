
package ee.cone.base.test_loots

import ee.cone.base.connection_api.Obj

// to app

trait Measure {
  def apply[T](activity: ()=>T)(handlePeriod: (Long,Long)⇒Unit): T
}
