package ee.cone.base.util

import java.security.MessageDigest

object UInt {
  def apply(b: Array[Byte], pos: Int) = b(pos) & 0xFF
}

object Hex {
  val mapping = "0123456789ABCDEF".toCharArray
  def apply(bytes: Array[Byte]): String = {
    val res = new Array[Char](bytes.length * 2)
    var j = 0
    while(j < bytes.length){
      val v = UInt(bytes, j)
      res(j * 2) = mapping(v >>> 4)
      res(j * 2 + 1) = mapping(v & 0x0F)
      j = j + 1
    }
    new String(res)
  }
}

/*object HexDebug {
  def apply(i: Long) = "0x%04x".format(i)
}*/

object MD5 {
  def apply(bytes: Array[Byte]): String = Hex(MessageDigest.getInstance("MD5").digest(bytes))
}
