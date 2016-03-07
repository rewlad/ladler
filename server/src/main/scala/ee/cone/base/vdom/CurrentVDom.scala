package ee.cone.base.vdom

import ee.cone.base.connection_api._
import ee.cone.base.util.{Never, Single}

class CurrentVDom(
  handlerLists: CoHandlerLists,
  diff: Diff,
  jsonToString: JsonToString,
  sender: SenderOfConnection,
  wasNoValue: WasNoValue
) extends CurrentView with CoHandlerProvider {
  def invalidate() = vDom = wasNoValue
  def until(value: Long) = if(value < until) until = value
  private var until: Long = Long.MaxValue
  private var vDom: Value = wasNoValue
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

  def handlers = CoHandler(FromAlienDictMessageKey){ messageOpt =>
    messageOpt.foreach{ message => //dispatches incoming message // can close / set refresh time
      relocate(message)
      dispatch(message)
    }
    if(until <= System.currentTimeMillis) invalidate()
    if(vDom == wasNoValue){
      until = Long.MaxValue
      vDom = view(hashForView,"")
      diff.diff(vDom).foreach(d=>sender.sendToAlien("showDiff", jsonToString(d)))
    }
  } :: Nil
  private lazy val PathSplit = """(.*)(/[^/]*)""".r
  private def view(pathPrefix: String, pathPostfix: String): Value =
    Single.option(handlerLists.list(ViewPath(pathPrefix))).map(_(pathPostfix))
      .getOrElse{
        val PathSplit(nextPrefix,nextPostfix) = pathPrefix
        view(nextPrefix,s"$nextPostfix$pathPostfix")
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