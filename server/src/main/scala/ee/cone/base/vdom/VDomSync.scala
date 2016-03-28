package ee.cone.base.vdom

import java.util.{UUID, Base64}

import ee.cone.base.connection_api._
import ee.cone.base.util.{Never, UTF8String}
import ee.cone.base.vdom.Types._

class AlienAttrFactoryImpl(handlerLists: CoHandlerLists, currentVDom: CurrentVDom) extends AlienAttrFactory {
  def apply[Value](attr: Attr[Value]): UUID => Value => Unit = { //when handlers are are available
    val addEvent = handlerLists.single(AddChangeEvent(attr))
    (srcId: UUID) => // when making input tag
    newValue =>
    addEvent(srcId,newValue)
    currentVDom.invalidate()
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

object WasNoValueImpl extends WasNoVDomValue {
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