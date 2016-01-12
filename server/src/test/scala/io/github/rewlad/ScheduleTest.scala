package io.github.rewlad

import java.util.concurrent.{TimeUnit, ScheduledFuture, Executors}
import java.util.concurrent.atomic.AtomicInteger

import io.github.rewlad.sseserver.ToRunnable

object Test1 extends App {
  val pool = Executors.newScheduledThreadPool(5)
  val aCount = new AtomicInteger(0)
  val bCount = new AtomicInteger(0)
  val aFuture: ScheduledFuture[_] = pool.scheduleAtFixedRate(ToRunnable{
    println(s"a:${aCount.incrementAndGet()}")
    if(aCount.get==3){ // skipped iterations
      Thread.sleep(3000)
      println(s"a:exit")
    }
    if(aCount.get==7){
      aFuture.cancel(false) // true'll cause interrupt during the sleep
      Thread.sleep(1)
      println(s"a:cancel")
    }
  },100,1000,TimeUnit.MILLISECONDS)
  pool.scheduleAtFixedRate(ToRunnable{
    println(s"b:${bCount.incrementAndGet()}")
    if(bCount.get==8){
      throw new Exception("b failed") // will not run next time
    }
  },200,1000,TimeUnit.MILLISECONDS)
}

object Test2 extends App {
  val pool = Executors.newScheduledThreadPool(5)
  val aCount = new AtomicInteger(0)
  val aFuture: ScheduledFuture[_] = pool.scheduleAtFixedRate(ToRunnable{
    println(s"a:${aCount.incrementAndGet()}")
    if(aCount.get==3) throw new Exception("b failed") // aFuture.isDone 'll be true after that
  },100,1000,TimeUnit.MILLISECONDS)
  pool.scheduleAtFixedRate(ToRunnable{
    println(s"${aFuture.isCancelled}:${aFuture.isDone}")
  },200,1000,TimeUnit.MILLISECONDS)
}
