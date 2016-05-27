package ee.cone.base.vdom

import ee.cone.base.connection_api.CoHandlerLists
import ee.cone.base.vdom.Types._

trait OfDiv

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
case class TextContentElement(content: String) extends VDomValue {
  def appendJson(builder: JsonBuilder) = {
    builder.startObject()
    builder.append("tp").append("span")
    builder.append("content").append(content)
    builder.end()
  }
}

class TagsImpl(
  currentView: CurrentView,
  child: ChildPairFactory
) extends Tags {
  def root(children: List[ChildPair[OfDiv]]) =
    child("root", RootElement(currentView.rootAttributes), children.toList).value
  def text(key: VDomKey, text: String) =
    child[OfDiv](key, TextContentElement(text), Nil)
}
