
package ee.cone.base.db

object LongMix {
  private def blockBitSize = 16
  private def blockMask: Long = 0xFFFFL
  private def split(v: Int): Long = // makes 0x0000vvvv0000vvvv
    ((v.toLong & (blockMask << blockBitSize)) << blockBitSize) | (v.toLong & blockMask)
  def apply(a: Int, b: Int): Long = (split(a) << blockBitSize) | split(b)
}

class BlockIterator(actorId: Int, iterator: Iterator[Int]) extends Iterator[Long] {
  def this(actorId: Int) = this(actorId, (1 to Integer.MAX_VALUE).iterator)
  def next(): Long = LongMix(actorId, iterator.next())
  def hasNext = iterator.hasNext
}
