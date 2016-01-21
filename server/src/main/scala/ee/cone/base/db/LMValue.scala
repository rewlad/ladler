package ee.cone.base.db

import ee.cone.base.util.{Bytes, Never}

/*NoGEN*/ sealed abstract class LMValue {
  def allocWrite(spaceBefore: Int, spaceAfter: Int): Array[Byte]
}

/*NoGEN*/ abstract class LMSingleValue extends LMValue {
  protected def splitterB(exchangeA: LongByteExchange, spaceAfter: Int): Array[Byte] = {
    val exchangeB = LMBytes.`splitter`.after(exchangeA)
    exchangeA.writeHead(exchangeB.writeHead(exchangeB.alloc(spaceAfter)))
  }
}

case object LMRemoved extends LMValue {
  def allocWrite(spaceBefore: Int, spaceAfter: Int) = Never()
}

case class LMStringValue(value: String) extends LMSingleValue {
  def allocWrite(spaceBefore: Int, spaceAfter: Int) = {
    val src = Bytes(value)
    val exchangeA = LMBytes.toWrite(src).at(spaceBefore)
    //exchangeA.write(src, exchangeA.alloc(spaceAfter+1))
    exchangeA.write(src, splitterB(exchangeA,spaceAfter))
  }
}

case class LMLongValue(value: Long) extends LMSingleValue {
  def allocWrite(spaceBefore: Int, spaceAfter: Int) = {
    val exchangeA = LMBytes.toWrite(value).at(spaceBefore)
    exchangeA.write(value, splitterB(exchangeA,spaceAfter))
  }
}

case class LMLongPairValue(valueA: Long, valueB: Long) extends LMValue {
  def allocWrite(spaceBefore: Int, spaceAfter: Int) = {
    val exchangeA = LMBytes.toWrite(valueA).at(spaceBefore)
    val exchangeB = LMBytes.toWrite(valueB).after(exchangeA)
    exchangeA.write(valueA, exchangeB.write(valueB, exchangeB.alloc(spaceAfter)))
  }
}
