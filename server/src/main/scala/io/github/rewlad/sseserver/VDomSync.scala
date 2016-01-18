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

class ReactiveVDom(sender: SenderOfConnection){
  private var prevVDom: Value = WasNoValue
  def diffAndSend(vDom: Value) = {
    Diff(prevVDom, vDom).foreach { diff =>
      val builder = new JsonBuilderImpl
      diff.appendJson(builder)
      sender.send("showDiff", builder.toString)
      println(builder.toString)
    }
    prevVDom = vDom
  }
  private def find(value: Value, path: List[String]): Option[Value] =
    if(path.isEmpty) Some(value) else Some(value).collect{
      case m: MapValue => m.value.collectFirst{
        case pair if pair.jsonKey == path.head => find(pair.value, path.tail)
      }.flatten
    }.flatten
  def dispatch(messageOption: Option[ReceivedMessage]) =
    for(message <- messageOption; path <- message.value.get("X-r-vdom-path")){
      println(s"path ($path)")
      val "" :: parts = path.split("/").toList
      find(prevVDom, parts).collect{ case v: ElementValue => v }
        .getOrElse(throw new Exception(s"path ($path) was not found in ($prevVDom) "))
        .handleMessage(message)
    }
}
