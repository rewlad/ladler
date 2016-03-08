package ee.cone.base.vdom

import java.util.Base64

import ee.cone.base.connection_api.DictMessage
import ee.cone.base.util.{Never, UTF8String}

object InputAttributesImpl extends InputAttributes {
  def appendJson(builder: JsonBuilder, value: String, deferSend: Boolean): Unit = {
    builder.append("value").append(value)
    if(deferSend){
      builder.append("onChange").append("local")
      builder.append("onBlur").append("send")
    } else builder.append("onChange").append("send")
  }
}

object OnChangeImpl extends OnChange {
  def unapply(message: DictMessage): Option[String] = message match {
    case DictMessage(mv) if mv.getOrElse("X-r-action","") == "change" =>
      Some(UTF8String(Base64.getDecoder.decode(mv("X-r-vdom-value-base64"))))
    case _ => None
  }
}

object OnClickImpl extends OnClick {
  def unapply(message: DictMessage): Option[Unit] = message match {
    case DictMessage(mv) if mv.getOrElse("X-r-action","") == "click" => Some(())
    case _ => None
  }
}

object WasNoValueImpl extends WasNoValue {
  def appendJson(builder: JsonBuilder): Unit = Never()
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