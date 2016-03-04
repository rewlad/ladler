package ee.cone.base.test_react_db

import java.nio.file.Paths
import java.util.concurrent.locks.ReentrantLock


import ee.cone.base.connection_api._
import ee.cone.base.vdom._

import scala.collection.immutable.SortedMap

import ee.cone.base.db._
import ee.cone.base.db.Types._
import ee.cone.base.server._
import ee.cone.base.util.{Single, Never, Setup}

/*
class LifeCacheState[C] {
  private var state: Option[C] = None
  def apply() = state
  def set(lifeCycle: LifeCycle, value: =>C) = {
    if(state.nonEmpty) Never()
    lifeCycle.setup()(_ => state = None)
    state = Option(value)
  }
}

class LifeCache[C] {
  lazy val lifeCycle = new LifeCacheState[LifeCycle]
  def apply(create: =>C): ()=>C = {
    val base = new LifeCacheState[C]
    () =>
      if(base().isEmpty) base.set(lifeCycle().get, create)
      base().get
  }
}
*/

////

/*
class FindOrCreateSrcId(
  srcId: Attr[UUID],
  searchSrcId: ListByValue[UUID],
  seq: ObjIdSequence
) {
  def apply(value: UUID) = Single.option(searchSrcId.list(value))
    .getOrElse(Setup(seq.inc()){ node => node(srcId) = value })
}
*/

////



class TestAppMix extends ServerAppMix with DBAppMix {
  lazy val httpPort = 5557
  lazy val staticRoot = Paths.get("../client/build/test")
  lazy val ssePort = 5556
  lazy val threadCount = 5
  lazy val mainDB = new TestEnv
  lazy val instantDB = new TestEnv
  lazy val createConnection =
    (lifeCycle:LifeCycle) ⇒ new TestConnectionMix(this, lifeCycle)
}

class TestConnectionMix(
  app: TestAppMix, val lifeCycle: LifeCycle
) extends ServerConnectionMix with SessionDBConnectionMix with VDomConnectionMix {
  lazy val serverAppMix = app
  lazy val dbAppMix = app
  lazy val allowOrigin = Some("*")
  lazy val framePeriod = 200
  //lazy val mainDB = app.mainDB
  override def handlers =
    new FailOfConnection(sender).handlers :::
    new DynEdit().handlers :::
    super.handlers
}

object TestApp extends App {
  val app = new TestAppMix
  app.start()
  println(s"SEE: http://127.0.0.1:${app.httpPort}/react-app.html")
}

////

class TestEnv extends DBEnv {
  private var data = SortedMap[RawKey, RawValue]()(UnsignedBytesOrdering)
  private lazy val lock = new ReentrantLock
  private def createRawIndex() = Setup(new NonEmptyUnmergedIndex) { i =>
    synchronized {
      i.data = data
    }
  }
  def createTx(txLifeCycle: LifeCycle, rw: Boolean) =
    if(!rw) new RawTx(txLifeCycle, rw, createRawIndex(), () => ())
    else {
      txLifeCycle.onClose(()=>lock.unlock())
      lock.lock()
      val index = createRawIndex()
      def commit() = {
        if(!lock.isHeldByCurrentThread) Never()
        synchronized {
          data = index.data
        }
      }
      new RawTx(txLifeCycle, rw, index, commit)
    }
  def start() = ()
}

class FailOfConnection(
  sender: SenderOfConnection
) extends CoHandlerProvider {
  def handlers = CoHandler(FailEventKey){ e =>
    sender.send("fail",e.toString) //todo
  } :: Nil
}

class DynEdit(
  eventSourceOperations: SessionEventSourceOperations
) extends CoHandlerProvider {
  def handlers = CoHandler(ViewPath("/db")){ pf =>
    eventSourceOperations.incrementalApplyAndView{ ()⇒
      ???
    }
  } :: Nil
}







/*

  makeVDom() // send
}*/

//

////
