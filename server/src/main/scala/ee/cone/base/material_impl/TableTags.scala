package ee.cone.base.material_impl

import ee.cone.base.material.TableUtilTags
import ee.cone.base.vdom.Types._
import ee.cone.base.vdom._

class MaterialTableTags(
  wrapped: TableTags, tags: Tags, style: TagStyles, materialStyles: MaterialStyles
) extends TableTags {
  import tags._
  import materialStyles._
  def table(key: VDomKey, attr: TagAttr*)(children: List[ChildOfTable]) =
    List(div("table",MaterialFont)(
      wrapped.table(key,attr:_*)(children)
    ))
  def row(key: VDomKey, attr: TagAttr*)(children: List[ChildOfTableRow]) =
    wrapped.row(key,Divider(DividerElement()) :: attr.toList:_*)(children)

  private def align(textAlign: TagStyle)(children:List[ChildPair[OfDiv]]) =
    List(div("1",style.displayTable,style.widthAll,style.heightAll)(List(
      div("1",style.displayCell,style.widthAll,style.heightAll,style.alignMiddle,marginSide,textAlign)(children)
    )))

  def group(key: VDomKey, attr: TagAttr*)(children:List[ChildPair[OfDiv]]) =
    wrapped.group(key, attr:_*)(
      if(children.isEmpty) Nil else align(style.alignCenter)(children)
    )
  def cell(key: VDomKey, attr: TagAttr*)(children: CellContentVariant ⇒ List[ChildPair[OfDiv]]) =
    wrapped.cell(key, attr:_*)(showLabel⇒align(style.none)(children(showLabel)))
}

class TableUtilTagsImpl(
  tags: Tags,
  style: TagStyles,
  materialStyles: MaterialStyles
) extends TableUtilTags {
  import tags._
  def controlPanel(left: List[ChildPair[OfDiv]], right: List[ChildPair[OfDiv]]): List[ChildPair[OfDiv]] =
    List(div("tableControl",style.padding(5))(List(div("1",materialStyles.paddingSide(8))(List(
      div("1",style.displayInlineBlock,style.minWidth(1),style.maxWidth(1),style.minHeight(48))(Nil),
      div("2",style.displayInlineBlock,OnlyWidthPercentTagStyle(60))(left),
      div("3",RightFloatTagStyle)(right)
    )))))
}

case class DividerElement() extends VDomValue{
  def appendJson(builder: JsonBuilder)={
    builder.startObject()
    builder.append("tp").append("hr")
    builder.append("style").startObject()
    builder.append("didFlip").append(true)
    builder.append("margin").append("0px")
    builder.append("marginTop").append("0px")
    builder.append("marginLeft").append("0px")
    builder.append("height").append("1px")
    builder.append("border").append("none")
    builder.append("borderBottom").append("1px solid #e0e0e0")
    builder.end()
    builder.end()
  }
}

case class OnlyWidthPercentTagStyle(value: Int) extends TagStyle {
  def appendStyle(builder: JsonBuilder) = {
    builder.append("minWidth").append(s"$value%")
    builder.append("maxWidth").append(s"$value%")
  }
}

case object RightFloatTagStyle extends TagStyle {
  def appendStyle(builder: JsonBuilder) =
    builder.append("float").append("right")
}

case object MaterialFont extends TagStyle {
  def appendStyle(builder: JsonBuilder) = {
    //builder.append("fontSize").append(if(isHeader) "12px" else "13px")
    builder.append("fontWeight").append("500")
    builder.append("color").append("rgba(0,0,0,0.54)")
  }
}