package ee.cone.base.test_sse

import java.util.concurrent.{TimeUnit, BlockingQueue}
import ee.cone.base.connection_api.{DictMessage, Message}
import ee.cone.base.server._





/*
    while(true){
      show()
      val nextShowTime = System.currentTimeMillis + framePeriod
      while(nextShowTime > System.currentTimeMillis || vDomIsInvalid.value){
        receive(queue.poll(framePeriod,TimeUnit.MILLISECONDS))
      }
      //keepAlive.receive()
      //Thread.sleep(100)
    }*/
  }

  private var prevTime: Long = 0L
  def receive = {
    case PeriodicMessage =>
      if(true){
        val time: Long = System.currentTimeMillis
        sender.send("show",s"$time")
      } else {
        val time: Long = System.currentTimeMillis / 100
        if(prevTime != time) {
          prevTime = time
          sender.send("show",s"$time")
        }
      }
    case _ => ()
  }
}
