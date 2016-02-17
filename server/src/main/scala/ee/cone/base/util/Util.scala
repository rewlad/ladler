package ee.cone.base.util

import java.nio.charset.StandardCharsets.UTF_8
import java.security.MessageDigest

object Bytes {
  def apply(content: String) = content.getBytes(UTF_8)
}

object UTF8String {
  def apply(data: Array[Byte]) = new String(data,UTF_8)
}

object ToRunnable {
  def apply(f: => Unit) = new Runnable() { def run() { f } }
}

object Single {
  def apply[C](l: List[C]): C = l match {
    case el :: Nil => el
    case _ => throw new Exception()
  }
  def option[C](l: List[C]): Option[C] = if(l.isEmpty) None else Option(apply(l))
}

object Setup {
  def apply[T](o: T)(f: T=>Unit): T = { f(o); o }
}

object Trace {
  def apply[T](f: =>T) = try { f } catch {
    case e: Throwable => e.printStackTrace(); throw e
  }
}

object Never{
  def apply():Nothing = throw new Exception("Never Here")
}

object LongFits {
  def apply(value: Long, sz: Int, isUnsigned: Boolean, offset: Int): Long = {
    if (sz <= 0 || sz > java.lang.Long.SIZE) Never()
    val bitUnSize = java.lang.Long.SIZE - sz // << 64 does not work
    val touchedValue = if(isUnsigned) (value << bitUnSize) >>> bitUnSize else (value << bitUnSize) >> bitUnSize
    if (value != touchedValue) Never()
    value << offset
  }
}
