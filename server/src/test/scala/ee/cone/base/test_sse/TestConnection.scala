package ee.cone.base.test_sse

import ee.cone.base.connection_api._

class TestConnection() extends CoHandlerProvider {
  def handlers = CoHandler(ShowToAlien){ ()=>
    val time: Long = System.currentTimeMillis
    ("show",s"$time") :: Nil
  } :: Nil
}
