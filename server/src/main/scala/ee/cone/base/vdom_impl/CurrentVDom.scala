package ee.cone.base.vdom_impl

import java.util.{Base64, UUID}

import ee.cone.base.connection_api._
import ee.cone.base.util.{Never, Single, UTF8String}
import ee.cone.base.vdom._

class CurrentVDom(
  handlerLists: CoHandlerLists,
  diff: Diff,
  jsonToString: JsonToString,
  wasNoValue: WasNoVDomValue,
  child: ChildPairFactory
) extends CurrentView with CoHandlerProvider {
  def invalidate() = vDom = wasNoValue
  def until(value: Long) = if(value < until) until = value
  private var until: Long = Long.MaxValue
  private var vDom: VDomValue = wasNoValue
  private var hashForView = ""
  private var hashFromAlien = ""
  private def relocate(message: DictMessage): Unit =
    for(hash <- message.value.get("X-r-location-hash")) if(hashFromAlien != hash){
      hashFromAlien = hash
      relocate(hash)
    }
  def relocate(value: String) = if(hashForView != value) {
    hashForView = value
    println(s"hashForView: $value")
    invalidate()
  }
  private def dispatch(message: DictMessage): Unit =
    for(pathStr <- message.value.get("X-r-vdom-path")){
      val path = pathStr.split("/").toList match {
        case "" :: parts => parts
        case _ => Never()
      }
      def decoded = UTF8String(Base64.getDecoder.decode(message.value("X-r-vdom-value-base64")))
      (message.value.get("X-r-action"), ResolveValue(vDom, path)) match {
        case (Some("click"), Some(v: OnClickReceiver)) => v.onClick.get()
        case (Some("change"), Some(v: OnChangeReceiver)) => v.onChange.get(decoded)
        case (Some("resize"), Some(v: OnResizeReceiver)) => v.onResize.get(decoded)
        case v => throw new Exception(s"$path ($v) can not receive $message")
      }
    }
  private def switchSession(message: DictMessage) =
    for(sessionKey <- message.value.get("X-r-session")){
      handlerLists.list(SwitchSession).foreach(_(UUID.fromString(sessionKey)))
    }
  private def showToAlien() = {
    if(until <= System.currentTimeMillis) invalidate()
    if(vDom != wasNoValue) Nil else {
      until = Long.MaxValue
      vDom =
        child("root", RootElement(rootAttributes), view(hashForView,"")).asInstanceOf[VPair].value
      diff.diff(vDom).map(d=>("showDiff", jsonToString(d))).toList :::
        (if(hashFromAlien==hashForView) Nil else ("relocateHash",hashForView) :: Nil)
    }
  }
  def handlers =
    CoHandler(FromAlienDictMessage)(setLastMessage) ::
    CoHandler(FromAlienDictMessage)(switchSession) ::
    CoHandler(FromAlienDictMessage)(relocate) ::
    CoHandler(FromAlienDictMessage)(dispatch) ::    //dispatches incoming message // can close / set refresh time
    CoHandler(ShowToAlien)(showToAlien) ::
    Nil

  var rootAttributes: List[(String,List[String])] = Nil
  private def setLastMessage(message: DictMessage) = rootAttributes =
    List("ackMessage"→List("ackMessage",message.value("X-r-connection"),message.value("X-r-index")))

  private lazy val PathSplit = """(.*)(/[^/]*)""".r
  private def view(pathPrefix: String, pathPostfix: String): List[ChildPair[_]] =
    Single.option(handlerLists.list(ViewPath(pathPrefix))).map(_(pathPostfix))
      .getOrElse(pathPrefix match {
        case PathSplit(nextPrefix,nextPostfix) =>
          view(nextPrefix,s"$nextPostfix$pathPostfix")
      })


}

case class RootElement(conf: List[(String,List[String])]) extends VDomValue {
  def appendJson(builder: JsonBuilder) = {
    builder.startObject()
    builder.append("tp").append("span")
    conf.foreach{ case (k,v) ⇒
      builder.append(k)
      builder.startArray()
      v.foreach(builder.append)
      builder.end()
    }
    builder.end()
  }
}

object ResolveValue {
  def apply(value: VDomValue, path: List[String]): Option[VDomValue] =
    if(path.isEmpty) Some(value) else Some(value).collect{
      case m: MapVDomValue => m.pairs.collectFirst{
        case pair if pair.jsonKey == path.head => apply(pair.value, path.tail)
      }.flatten
    }.flatten
}