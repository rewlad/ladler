package ee.cone.base.db

import ee.cone.base.connection_api.LifeCycle
import ee.cone.base.db.Types._
import ee.cone.base.util.Setup

import scala.collection.immutable.SortedMap

class InMemoryMergedIndex extends RawIndex {
  var peek: SeekStatus = NotFoundStatus
  var data = SortedMap[RawKey, RawValue]()(UnsignedBytesOrdering)
  private var iterator: Iterator[(RawKey, RawValue)] = VoidKeyIterator
  def set(key: RawKey, value: RawValue) = {
    //println(s"uu -- ${Hex(key)} -- ${Hex(value)} -- ${value.length}")
    if(value.length > 0) data = data + (key -> value) else data = data - key
  }

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

class InMemoryEnv[DBEnvKey](val dbId: Long) extends DBEnv[DBEnvKey] {
  var data = SortedMap[RawKey, RawValue]()(UnsignedBytesOrdering)
  private def createRawIndex() = Setup(new InMemoryMergedIndex) { i =>
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
