package ee.cone.base.vdom

import java.util.Base64

import ee.cone.base.connection_api._
import ee.cone.base.util.{Single, Never, UTF8String}
import ee.cone.base.vdom.Types._

class AlienAttrFactoryImpl(handlerLists: CoHandlerLists, currentVDom: CurrentVDom) extends AlienAttrFactory {
  def apply[Value](attr: Attr[Value], key: VDomKey) = { //when handlers are are available
    val eventAdder = Single(handlerLists.list(ChangeEventAdder(attr)))
    new Attr[AlienRef[Value]] {
      def defined = Never()
      def get(node: Obj) = { // when making input tag
        val addEvent = eventAdder(node) // remembers srcId
        AlienRef(key, node(attr)){ newValue =>
          addEvent(newValue)
          currentVDom.invalidate()
        }
      }
      def set(node: Obj, value: AlienRef[Value]) = Never()
    }
  }
}

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