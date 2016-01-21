
package ee.cone.base.db

import ee.cone.base.util.Never
import ee.cone.base.db.LMTypes._

import scala.collection.immutable.SortedMap

/*NoGEN*/ class EmptyUnmergedTx extends RawTx {
  override def set(key: LMKey, value: LMRawValue) = Never()
  override def get(key: LMKey) = Never()
  override def peek = NotFoundStatus
  override def seek(from: LMKey) = ()
  override def seekNext() = ()
}

/*NoGEN*/ class NonEmptyUnmergedTx extends RawTx {
  var peek: SeekStatus = NotFoundStatus
  var data = SortedMap[LMKey, LMRawValue]()(UnsignedBytesOrdering)
  private var iterator: Iterator[(LMKey, LMRawValue)] = VoidKeyIterator
  def set(key: LMKey, value: LMRawValue) = data = data + (key -> value)
  def get(key: LMKey) = data(key)
  def seek(from: LMKey) = {
    iterator = data.from(from).iterator
    seekNext()
  }
  def seekNext() = {
    peek = if(iterator.hasNext)
      iterator.next() match { case (k,v) ⇒ new KeyStatus(k, v) }
    else NotFoundStatus
  }
}

class MuxUnmergedTx(var unmerged: RawTx, merged: RawTx) extends RawTx {
  var peek: SeekStatus = NotFoundStatus
  def get(key: LMKey): LMRawValue = unmerged match {
    case tx: NonEmptyUnmergedTx ⇒ tx.data.getOrElse(key,merged.get(key))
    case _ ⇒ merged.get(key)
  }
  def set(key: LMKey, value: LMRawValue): Unit = {
    unmerged match {
      case tx: NonEmptyUnmergedTx ⇒ ()
      case _ ⇒ unmerged = new NonEmptyUnmergedTx
    }
    unmerged.set(key, value)
  }
  private def compare(a: SeekStatus, b: SeekStatus): Int = {
    if(a eq b){ return 0 }
    val d = java.lang.Boolean.compare(!a.isInstanceOf[KeyStatus], !b.isInstanceOf[KeyStatus])
    if(d==0) UnsignedBytesOrdering.compare(a.key, b.key) else d
  }
  private def skipSame(tx: RawTx) = tx.peek match {
    case p: KeyStatus if compare(peek, p)==0 ⇒ tx.seekNext()
    case _ ⇒ ()
  }
  private def chooseMin(): Unit = {
    peek = if(compare(unmerged.peek, merged.peek)<0) unmerged.peek else merged.peek
    peek match {
      case p: KeyStatus if p.value.isEmpty ⇒ seekNext()
      case _ ⇒ ()
    }
  }
  def seek(from: LMKey) = {
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

object VoidKeyIterator extends Iterator[(LMKey, LMRawValue)] {
  def hasNext = false
  def next() = Never()
}
