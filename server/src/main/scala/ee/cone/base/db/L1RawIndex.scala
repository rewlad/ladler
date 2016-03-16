
package ee.cone.base.db

import ee.cone.base.util.{UInt, Never}
import ee.cone.base.db.Types._

import scala.collection.immutable.SortedMap

class EmptyUnmergedIndex(merged: RawIndex) extends RawIndex {
  def set(key: RawKey, value: RawValue) = Never()
  def get(key: RawKey) = merged.get(key)
  def peek = NotFoundStatus
  def seek(from: RawKey) = ()
  def seekNext() = ()
}

class NonEmptyUnmergedIndex(pass: RawKey=>RawValue) extends RawIndex {
  var peek: SeekStatus = NotFoundStatus
  var data = SortedMap[RawKey, RawValue]()(UnsignedBytesOrdering)
  private var iterator: Iterator[(RawKey, RawValue)] = VoidKeyIterator
  def set(key: RawKey, value: RawValue) = data = data + (key -> value)
  def get(key: RawKey) = data.getOrElse(key,pass(key))
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

class MuxUnmergedIndex(var unmerged: RawIndex, val merged: RawIndex) extends RawIndex {
  var peek: SeekStatus = NotFoundStatus
  def get(key: RawKey): RawValue = unmerged.get(key)
  def set(key: RawKey, value: RawValue): Unit = {
    unmerged match {
      case tx: NonEmptyUnmergedIndex ⇒ ()
      case _ ⇒ unmerged = new NonEmptyUnmergedIndex(merged.get)
    }
    unmerged.set(key, value)
  }
  private def compare(a: SeekStatus, b: SeekStatus): Int = {
    if(a eq b){ return 0 }
    val d = java.lang.Boolean.compare(!a.isInstanceOf[KeyStatus], !b.isInstanceOf[KeyStatus])
    if(d==0) UnsignedBytesOrdering.compare(a.key, b.key) else d
  }
  private def skipSame(tx: RawIndex) = tx.peek match {
    case p: KeyStatus if compare(peek, p)==0 ⇒ tx.seekNext()
    case _ ⇒ ()
  }
  private def chooseMin(): Unit = {
    peek = if(compare(unmerged.peek, merged.peek)<=0) unmerged.peek else merged.peek // <= unmerged value will have priority
    peek match {
      case p: KeyStatus if p.value.isEmpty ⇒ seekNext()
      case _ ⇒ ()
    }
  }
  def seek(from: RawKey) = {
    unmerged.seek(from)
    merged.seek(from)
    chooseMin()
  }
  def seekNext() = {
    skipSame(unmerged)
    skipSame(merged)
    chooseMin()
  }
}

object VoidKeyIterator extends Iterator[(RawKey, RawValue)] {
  def hasNext = false
  def next() = Never()
}

object UnsignedBytesOrdering extends math.Ordering[Array[Byte]] {
  def compare(a: Array[Byte], b: Array[Byte]): Int = {
    val default = java.lang.Integer.compare(a.length,b.length)
    val len = Math.min(a.length,b.length)
    var i = 0
    while(i < len){
      val d = java.lang.Integer.compare(UInt(a,i), UInt(b,i))
      if(d != 0) return d
      i += 1
    }
    default
  }
}

class MuxFactoryImpl extends MuxFactory {
  override def wrap(rawIndex: RawIndex): RawIndex =
    new MuxUnmergedIndex(new EmptyUnmergedIndex(rawIndex),rawIndex)
}
