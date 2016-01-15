package io.github.rewlad.sseserver

import java.util.Base64

object Input {
  def appendJsonAttributes(builder: JsonBuilder, value: String, deferSend: Boolean): Unit = {
    builder.append("value").append(value)
    if(deferSend){
      builder.append("onChange").append("local")
      builder.append("onBlur").append("send")
    } else builder.append("onChange").append("send")
  }
  def changedValueFromMessage(message: ReceivedMessage, onChange: String=>Unit): Boolean =
    ActionOf(message) match {
      case "change" =>
        onChange(UTF8String(Base64.getDecoder.decode(message.value("X-r-vdom-value-base64"))))
        true
      case _ => false
    }
}

class VersionObserver(version: ()=>String) {
  private var prevVer: Option[String] = None
  def thenDo(f: =>Unit): Unit = {
    val nextVer = Some(version())
    if(prevVer.isEmpty || prevVer != nextVer){
      f
      prevVer = nextVer
    }
  }
}

class ReactiveVDom(sender: SenderOfConnection){
  private var prevVDom: Option[Pair] = None
  def diffAndSend(vDom: Pair) = {
    Diff(prevVDom, vDom).foreach { diff =>
      val builder = new JsonBuilderImpl
      MapValue(diff :: Nil).appendJson(builder)


      sender.send("showDiff", builder.toString)
      println(builder.toString)
    }
    prevVDom = Some(vDom)
  }
  private def find(pairs: List[Pair], path: List[String]): Value =
    pairs.collectFirst{
      case pair if pair.jsonKey == path.head => pair.value
    }.collect{
      case m: MapValue => find(m.value, path.tail)
      case v if path.tail.isEmpty => v
    }.getOrElse(
      throw new Exception(s"path ($path) was not found in branch ($pairs) ")
    )

  def dispatch(messageOption: Option[ReceivedMessage]) =
    for(message <- messageOption; path <- message.value.get("X-r-vdom-path")){
      println(s"path ($path)")
      val "" :: parts = path.split("/").toList
      val attrs = find(prevVDom.get :: Nil, parts) match { case v: ElementValue => v }
      attrs.handleMessage(message)
    }
}
