package ee.cone.base.test_react_db

import java.nio.file.Paths
import java.util.UUID
import java.util.concurrent.{TimeUnit, BlockingQueue}
import java.util.concurrent.locks.ReentrantLock


import ee.cone.base.connection_api.{AppComponent, ConnectionComponent,
DictMessage, Message}

import scala.collection.immutable.SortedMap

import ee.cone.base.db._
import ee.cone.base.db.Types._
import ee.cone.base.server._
import ee.cone.base.util.{Single, Never, Setup}

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

////

class FindOrCreateSrcId(
  srcId: Attr[UUID],
  searchSrcId: ListByValue[UUID],
  seq: ObjIdSequence
) {
  def apply(value: UUID) = Single.option(searchSrcId.list(value))
    .getOrElse(Setup(seq.inc()){ node => node(srcId) = value })
}
class ObjIdSequence(seq: Attr[Option[DBNode]]) {
  def inc(): DBNode = {
    val node = new DBNodeImpl(0L)
    val res = new DBNodeImpl(node(seq).getOrElse(node).objId + 1L)
    node(seq) = Some(res)
    res
  }
}

////

class RawTx(val rawIndex: RawIndex, val commit: ()=>Unit)
trait DBEnv {
  def createTx(txLifeCycle: LifeCycle, rw: Boolean): RawTx
}

class TestEnv extends DBEnv {
  private var data = SortedMap[RawKey, RawValue]()(UnsignedBytesOrdering)
  private lazy val lock = new ReentrantLock
  private def createRawIndex() = Setup(new NonEmptyUnmergedIndex) { i =>
    synchronized {
      i.data = data
    }
  }
  def createTx(txLifeCycle: LifeCycle, rw: Boolean) =
    if(!rw) new RawTx(createRawIndex(), () => ())
    else txLifeCycle.setup {
      lock.lock()
      val index = createRawIndex()
      def commit() = {
        if(!lock.isHeldByCurrentThread) Never()
        synchronized {
          data = index.data
        }
      }
      new RawTx(index, commit)
    }(_=>lock.unlock())
}
/*
object TestApp extends App {
  val tempDB = new TestEnv
  val mainDB = new TestEnv

}
*/
class TestAppMix(val args: List[AppComponent]) extends ServerAppMix {
  lazy val httpPort = 5557
  lazy val staticRoot = Paths.get("../client/build/test")
  lazy val ssePort = 5556
  lazy val threadCount = 5
  lazy val createSSEConnection = (cArgs:List[ConnectionComponent]) ⇒ new TestConnectionMix(this, cArgs)
}

class TestConnectionMix(app: TestAppMix, val args: List[ConnectionComponent]) extends ServerConnectionMix with Runnable {
  lazy val serverAppMix = app
  lazy val allowOrigin = Some("*")
  lazy val framePeriod = 200
  lazy val run = new TestConnection(connectionLifeCycle,sender,incoming,framePeriod)
}

object TestApp extends App {
  val app = new TestAppMix(Nil)
  app.start()
  println(s"SEE: http://127.0.0.1:${app.httpPort}/react-app.html")
}

class TestConnection(
  connectionLifeCycle: LifeCycle,
  sender: SenderOfConnection,
  incoming: BlockingQueue[DictMessage],
  framePeriod: Long
  //, keepAlive: KeepAlive, queue: BlockingQueue[DictMessage],
  //
) {
  def apply(): Unit = try {
    connectionLifeCycle.open()
    while(true){
      //snapshot.init
      while(???/*snapshot.isOpenFresh*/){
        //incrementalApply
        //show
        while(???/*vDom.isOpenFresh*/){
          val message = Option(incoming.poll(framePeriod,TimeUnit.MILLISECONDS))
          //dispatch // can close / set refresh time
        }
      }
    }
  } catch {
    case e: Exception ⇒
      sender.send("fail",???)
      throw e
  } finally {
    connectionLifeCycle.close()
  }
}