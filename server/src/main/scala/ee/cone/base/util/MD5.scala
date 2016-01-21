package ee.cone.base.util

import java.security.MessageDigest

object MD5 {
  def apply(s: String): String = apply(Bytes(s))
  def apply(bytes: Array[Byte]): String = Hex(MessageDigest.getInstance("MD5").digest(bytes))
}
