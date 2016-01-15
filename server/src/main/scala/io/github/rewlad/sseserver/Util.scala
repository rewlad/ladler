package io.github.rewlad.sseserver

import java.nio.charset.StandardCharsets.UTF_8

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