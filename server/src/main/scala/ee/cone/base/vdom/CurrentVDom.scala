package ee.cone.base.vdom

import java.util.UUID

import ee.cone.base.connection_api._
import ee.cone.base.util.{Never, Single}

class CurrentVDom(
  handlerLists: CoHandlerLists,
  diff: Diff,
  jsonToString: JsonToString,
  wasNoValue: WasNoVDomValue
) extends CurrentView with CoHandlerProvider {
  def invalidate() = vDom = wasNoValue
  def until(value: Long) = if(value < until) until = value
  private var until: Long = Long.MaxValue
  private var vDom: VDomValue = wasNoValue
  private var hashForView = ""
  private def relocate(message: DictMessage): Unit =
    for(hash <- message.value.get("X-r-location-hash"))
      if(hashForView != hash) {
        hashForView = hash
        invalidate()
      }
  private def dispatch(message: DictMessage): Unit =
    for(pathStr <- message.value.get("X-r-vdom-path")){
      val path = pathStr.split("/").toList match {
        case "" :: parts => parts
        case _ => Never()
      }
      ResolveValue(vDom, path) match {
        case Some(v: MessageReceiver) => v.receive(message)
        case None => throw new Exception(s"$path not found")
        case _ => Never()
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
      vDom = view(hashForView,"")
      diff.diff(vDom).map(d=>("showDiff", jsonToString(d))).toList
    }
  }
  def handlers =
    CoHandler(FromAlienDictMessage)(switchSession) ::
    CoHandler(FromAlienDictMessage)(relocate) ::
    CoHandler(FromAlienDictMessage)(dispatch) ::    //dispatches incoming message // can close / set refresh time
    CoHandler(ShowToAlien)(showToAlien) ::
    Nil
  private lazy val PathSplit = """(.*)(/[^/]*)""".r
  private def view(pathPrefix: String, pathPostfix: String): VDomValue =
    Single.option(handlerLists.list(ViewPath(pathPrefix))).map(_(pathPostfix))
      .getOrElse(pathPrefix match {
        case PathSplit(nextPrefix,nextPostfix) =>
          view(nextPrefix,s"$nextPostfix$pathPostfix")
      })
}

object ResolveValue {
  def apply(value: VDomValue, path: List[String]): Option[VDomValue] =
    if(path.isEmpty) Some(value) else Some(value).collect{
      case m: MapVDomValue => m.pairs.collectFirst{
        case pair if pair.jsonKey == path.head => apply(pair.value, path.tail)
      }.flatten
    }.flatten
}