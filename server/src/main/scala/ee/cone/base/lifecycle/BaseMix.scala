package ee.cone.base.lifecycle

import ee.cone.base.connection_api.{AppMixBase, CoMixBase}

trait BaseAppMix extends AppMixBase {
  def threadCount: Int
  lazy val executionManager = new ExecutionManagerImpl(threadCount)
  def start() = toStart.foreach{ server ⇒
    server.start()
  }
}

trait BaseConnectionMix extends CoMixBase {
  lazy val handlerLists = new CoHandlerListsImpl(()⇒this)
}
