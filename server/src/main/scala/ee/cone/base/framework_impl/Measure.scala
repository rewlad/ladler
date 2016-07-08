
package ee.cone.base.framework_impl

class MeasureImpl extends Measure {
  def apply[T](activity: ()=>T)(handlePeriod: (Long,Long)⇒Unit) = {
    val startTime = System.currentTimeMillis
    val res = activity()
    val endTime = System.currentTimeMillis
    handlePeriod(startTime,endTime)
    res
  }
}
