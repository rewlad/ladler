package ee.cone.base.vdom

import java.util.Base64

import ee.cone.base.connection_api.ReceivedMessage
import ee.cone.base.util.UTF8String


object Input {
  def appendJsonAttributes(builder: JsonBuilder, value: String, deferSend: Boolean): Unit = {
    builder.append("value").append(value)
    if(deferSend){
      builder.append("onChange").append("local")
      builder.append("onBlur").append("send")
    } else builder.append("onChange").append("send")
  }
  def changedValue(message: ReceivedMessage) =
    UTF8String(Base64.getDecoder.decode(message.value("X-r-vdom-value-base64")))
}

class VersionObserver {
  private var prevVer: Option[String] = None
  def apply(version: String)(f: =>Unit): Unit = {
    val nextVer = Some(version)
    if(prevVer.isEmpty || prevVer != nextVer){
      f
      prevVer = nextVer
    }
  }
}

class Dispatch {
  var vDom: Value = WasNoValue
  var hashForView = ""
  private def find(value: Value, path: List[String]): Option[Value] =
    if(path.isEmpty) Some(value) else Some(value).collect{
      case m: MapValue => m.pairs.collectFirst{
        case pair if pair.jsonKey == path.head => find(pair.value, path.tail)
      }.flatten
    }.flatten
  def apply(messageOption: Option[ReceivedMessage]) = {
    for(message <- messageOption; path <- message.value.get("X-r-vdom-path")) {
      println(s"path ($path)")
      val "" :: parts = path.split("/").toList
      find(vDom, parts).collect { case v: MessageHandler => v }
        .getOrElse(throw new Exception(s"path ($path) was not found in ($vDom) "))
        .handleMessage(message)
      vDom = WasNoValue
    }
    for(message <- messageOption; hash <- message.value.get("X-r-location-hash"))
      if(hash != hashForView) {
        hashForView = hash
        vDom = WasNoValue
      }
  }
}
