package ee.cone.base.test_react_db

import java.nio.file.Paths
import java.util.UUID
import java.util.concurrent.locks.ReentrantLock
import scala.collection.immutable.SortedMap

import ee.cone.base.db._
import ee.cone.base.db.Types._
import ee.cone.base.server.{ContextOfConnection, SSEHttpServer, LifeCycle}
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
  srcId: Prop[UUID],
  searchSrcId: ListByValue[UUID],
  seq: ObjIdSequence
) {
  def apply(value: UUID) = Single.option(searchSrcId.list(value))
    .getOrElse(Setup(seq.inc()){ node => node(srcId) = value })
}
class ObjIdSequence(seq: Prop[Option[DBNode]]) {
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

object TestApp extends App {
  val tempDB = new TestEnv
  val mainDB = new TestEnv
  val server = new SSEHttpServer {
    def threadCount = 5
    def allowOrigin = Some("*")
    def ssePort = 5556
    def httpPort = 5557
    def framePeriod = 20
    def purgePeriod = 2000
    def staticRoot = Paths.get("../client/build/test")
    def createMessageReceiverOfConnection(context: ContextOfConnection) =
      new TestConnection(context, tempDB, mainDB)
  }
  server.start()
  println(s"SEE: http://127.0.0.1:${server.httpPort}/react-app.html")
}

