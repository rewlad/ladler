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
case class TextContentElement(content: String, color:String) extends VDomValue {
  def appendJson(builder: JsonBuilder) = {
    builder.startObject()
    builder.append("tp").append("span")
    builder.append("content").append(content)
    if(color.nonEmpty) {
      builder.append("style").startObject()
      builder.append("color").append(color)
      builder.end()
    }
    builder.end()
  }
}

class TagsImpl(
  currentView: CurrentView,
  child: ChildPairFactory
) extends Tags {
  def root(children: List[ChildPair[OfDiv]]) =
    child("root", RootElement(currentView.rootAttributes), children.toList).value
  def text(key: VDomKey, text: String, color:String = "") =
    child[OfDiv](key, TextContentElement(text,color), Nil)

}
