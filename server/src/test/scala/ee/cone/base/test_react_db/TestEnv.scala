package ee.cone.base.test_react_db

import scala.collection.immutable.SortedMap

import ee.cone.base.connection_api.LifeCycle
import ee.cone.base.db._
import ee.cone.base.db.Types._
import ee.cone.base.util.Setup

class TestIndex extends RawIndex {
  var peek: SeekStatus = NotFoundStatus
  var data = SortedMap[RawKey, RawValue]()(UnsignedBytesOrdering)
  private var iterator: Iterator[(RawKey, RawValue)] = VoidKeyIterator
  def set(key: RawKey, value: RawValue) = data = data + (key -> value)
  def get(key: RawKey) = data.getOrElse(key,Array())
  def seek(from: RawKey) = {
    iterator = data.from(from).iterator
    seekNext()
  }
  def seekNext() = {
    peek = if(iterator.hasNext)
      iterator.next() match { case (k,v) ⇒ new KeyStatus(k, v) }
    else NotFoundStatus
  }
}

class TestEnv[DBEnvKey](val dbId: Long) extends DBEnv[DBEnvKey] {
  private var data = SortedMap[RawKey, RawValue]()(UnsignedBytesOrdering)
  private def createRawIndex() = Setup(new TestIndex) { i =>
    synchronized { i.data = data }
  }
  def roTx(txLifeCycle: LifeCycle) = createRawIndex()
  private object RW
  def rwTx[R](f: RawIndex ⇒ R): R = RW.synchronized{
    val index = createRawIndex()
    Setup(f(index))(_ ⇒ synchronized { data = index.data })
  }
  def start() = ()
}
