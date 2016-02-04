package ee.cone.base.vdom

import java.util.Base64

import ee.cone.base.connection_api.{Message, DictMessage}
import ee.cone.base.util.UTF8String


object Input {
  def appendJsonAttributes(builder: JsonBuilder, value: String, deferSend: Boolean): Unit = {
    builder.append("value").append(value)
    if(deferSend){
      builder.append("onChange").append("local")
      builder.append("onBlur").append("send")
    } else builder.append("onChange").append("send")
  }
}

object OnChange {
  def unapply(message: Message): Option[String] = message match {
    case DictMessage(mv) if mv.getOrElse("X-r-action","") == "change" =>
      Some(UTF8String(Base64.getDecoder.decode(mv("X-r-vdom-value-base64"))))
    case _ => None
  }
}

object OnClick {
  def unapply(message: Message): Option[Unit] = message match {
    case DictMessage(mv) if mv.getOrElse("X-r-action","") == "click" => Some(())
    case _ => None
  }
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

trait MessageTransformer {
  def transformMessage: PartialFunction[DictMessage,Message]
}

object WithVDomPath {
  def unapply(message: DictMessage): Option[List[String]] =
    message.value.get("X-r-vdom-path").map(_.split("/").toList).collect{
      case "" :: parts => parts
    }
}

object ResolveValue {
  def apply(value: Value, path: List[String]): Option[Value] =
    if(path.isEmpty) Some(value) else Some(value).collect{
      case m: MapValue => m.pairs.collectFirst{
        case pair if pair.jsonKey == path.head => apply(pair.value, path.tail)
      }.flatten
    }.flatten
}
/*
def apply(path: List[String], message: Message): Option[Message] = {

        val node = find(vDom, parts).collect { case v: MessageTransformer => v }
        node.flatMap(_.transformMessage.lift(message))
  }
 */
//var hashForView = ""
/*.getOrElse(throw new Exception(s"path ($path) was not found in ($vDom) "))
          .receive(message)
        vDom = WasNoValue*/
/*
for(hash <- mv.get("X-r-location-hash") if hash != hashForView) {
  hashForView = hash
  vDom = WasNoValue
}*/