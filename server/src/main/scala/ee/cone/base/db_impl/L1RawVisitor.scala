package ee.cone.base.db_impl

import ee.cone.base.db.{KeyStatus, RawIndex}
import ee.cone.base.db.Types._

class RawVisitorImpl extends RawVisitor {
  def execute(tx: RawIndex, keyPrefix: RawKey, feed: RawKey⇒Boolean): Unit = {
    while(tx.peek match {
      case ks : KeyStatus
        if BytesSame.part(keyPrefix, ks.key) >= keyPrefix.length && feed(ks.key) ⇒
        tx.seekNext()
        true
      case _ ⇒ false
    }) {}
  }
}

object BytesSame {
  def part(a: Array[Byte], b: Array[Byte]): Int = {
    var i = 0
    while (i < a.length && i < b.length && a(i) == b(i)) i += 1
    i
  }
}
/*
class ObjIdExtractor[Value](
  rawConverter: RawConverter, converter: RawValueConverter[Value], skip: Int
) extends RawKeyExtractor { // 2/1
def apply(keyPrefix: RawKey, minSame: Int, b: RawKey, feed: Feed): Boolean = {
  val same = BytesSame.part(keyPrefix, b)
  if(same < minSame){ return false }
  feed(keyPrefix.length - same > 0, rawConverter.fromBytes(b,skip,converter,0))
  //
}
}
*/