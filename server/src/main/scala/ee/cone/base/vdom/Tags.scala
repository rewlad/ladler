package ee.cone.base.vdom

import ee.cone.base.vdom.Types._

trait OfDiv

case class WrappingElement(tp:String="span") extends VDomValue {
  def appendJson(builder: JsonBuilder) = {
    builder.startObject()
    builder.append("tp").append(tp)
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
  child: ChildPairFactory
) extends Tags {
  def root(children: List[ChildPair[OfDiv]]) =
    child("root", WrappingElement(), children.toList).value
  def text(key: VDomKey, text: String) =
    child[OfDiv](key, TextContentElement(text), Nil)
}
