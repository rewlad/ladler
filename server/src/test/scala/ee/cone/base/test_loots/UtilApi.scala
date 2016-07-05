
package ee.cone.base.test_loots // to app

trait Measure {
  def apply[T](activity: ()=>T)(handlePeriod: (Long,Long)⇒Unit): T
}
