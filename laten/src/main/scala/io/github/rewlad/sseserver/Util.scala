package io.github.rewlad.sseserver

import java.nio.charset.StandardCharsets._

object Bytes {
  def apply(content: String) = content.getBytes(UTF_8)
}

object ToRunnable {
  def apply(f: => Unit) = new Runnable() { def run() { f } }
}

object Single {
  def apply[C](l: List[C]): C = l match {
    case el :: Nil => el
  }
}

object Setup {
  def apply[T](o: T)(f: T=>Unit): T = { f(o); o }
}