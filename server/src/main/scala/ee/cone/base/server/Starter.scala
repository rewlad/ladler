package ee.cone.base.server

import java.util.concurrent.Executor
import ee.cone.base.util.ToRunnable

class Starter(components: List[AppComponent]) {
  def apply() = components.collect{ case c: CanStart => c.start() }
}

class ThreadPerSSEConnectionRun(
    pool: Executor, connectionLifeCycle: LifeCycle, runInner: ()⇒Unit
) {
  def apply() = pool.execute(ToRunnable{
    try{
      connectionLifeCycle.open()
      runInner()
    } finally connectionLifeCycle.close()
  })
}
