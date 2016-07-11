package ee.cone.base.vdom_impl

object JSONTest extends App {
  val builder = new JsonBuilderImpl()

  def make(left: Int): Unit = {
    if(left>0) {
      builder.append(left.toString)
      builder.append(left.toString)
      builder.append(left.toString)
      builder.startObject()
      make(left - 1)
      builder.end()
      builder.append(left.toString)
      builder.append(left.toString)
    }
  }

  make(63)
  println(builder.result.toString)
}
