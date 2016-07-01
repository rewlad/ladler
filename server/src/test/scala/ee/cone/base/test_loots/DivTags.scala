package ee.cone.base.test_loots

import ee.cone.base.vdom._
import ee.cone.base.vdom.Types._

case class DivButton()(val onClick:Option[()=>Unit]) extends VDomValue with OnClickReceiver {
  def appendJson(builder: JsonBuilder)={
    builder.startObject()
    builder.append("tp").append("div")
    onClick.foreach(_⇒ builder.append("onClick").append("send"))
    builder.append("style"); {
      builder.startObject()
      builder.append("cursor").append("pointer")
      builder.end()
    }
    builder.end()
  }
}

case class StyledDivWrapper(attr: List[TagStyle]) extends VDomValue {
  def appendJson(builder: JsonBuilder) = {
    builder.startObject()
    builder.append("tp").append("div")
    builder.append("style"); {
      builder.startObject()
      attr.foreach(_.appendStyle(builder))
      builder.end()
    }
    builder.end()
  }
}

class DivTags(child: ChildPairFactory){
  def div(key: VDomKey, attr: TagStyle*)(children: List[ChildPair[OfDiv]]): ChildPair[OfDiv] =
    child[OfDiv](key, StyledDivWrapper(attr.toList), children)
  def divButton(key:VDomKey)(action:()=>Unit)(children: List[ChildPair[OfDiv]])=
    child[OfDiv](key,DivButton()(Some(action)), children)
}

/*
remove:
divAlignWrapper

relate:
SVGIcon
StyledDivWrapper

change:
FlexDataTableTagsImpl
FlexDataTableHeader
 */