package ee.cone.base.material_impl

import ee.cone.base.vdom.Types._
import ee.cone.base.vdom._

case class FieldPopupDrop(opened:Boolean,maxHeight:Option[Int]=None) extends VDomValue{
  def appendJson(builder: JsonBuilder)={
    builder.startObject()
    builder.append("tp").append("FieldPopupDrop")
    builder.append("popupReg").append("def")
    builder.append("showDrop").append(opened)
    builder.append("visibility").append("hidden")
    maxHeight.foreach(v=>builder.append("maxHeight").append(s"${v}px"))
    builder.end()

  }
}

case class FieldPopupBox() extends VDomValue{
  def appendJson(builder: JsonBuilder)={
    builder.startObject()
    builder.append("tp").append("FieldPopupBox")
    builder.append("popupReg").append("def")
    builder.end()
  }
}

case class DivBgColorHover(color:Color) extends VDomValue{
  def appendJson(builder: JsonBuilder)={
    builder.startObject()
    builder.append("tp").append("CursorOver")
    builder.append("hoverColor").append(color.value)
    builder.end()
  }
}

case object MenuItemHoverColor extends Color { def value="rgba(0,0,0,0.1)" }

case object ClosedPopupState extends PopupState

class OptionTagsImpl(
  child: ChildPairFactory, tags: Tags, style: TagStyles
) extends OptionTags {
  import tags._
  private var opened: PopupState = ClosedPopupState
  private def divBgColorHover(key:VDomKey,color:Color)(children: List[ChildPair[OfDiv]])=
    child[OfDiv](key,DivBgColorHover(color),children)
  def option(key:VDomKey, caption: String)(activate: ()⇒Unit) =
    divButton(key){ () ⇒
      activate()
      opened = ClosedPopupState
    }(List(
      div("1",style.margin(5))(List(
        divBgColorHover("1",MenuItemHoverColor)(List(
          div("1",style.padding(10))(List(tags.text("1",caption)))
        ))
      ))
    ))
  def popupBox(key: VDomKey,fieldChildren:List[ChildPair[OfDiv]], popupChildren:List[ChildPair[OfDiv]]) = //cust
    div(key,style.displayBlock,style.widthAll,style.relative)(List(
      child[OfDiv](key+"box", FieldPopupBox(), fieldChildren),
      child[OfDiv](key+"popup", FieldPopupDrop(popupChildren.nonEmpty,maxHeight = Some(500)), popupChildren)
    ))
  def popupAction[Value](key: PopupState): (Boolean,()=>Unit) = {
    val isOpened = key == opened
    (isOpened, () => opened = if(isOpened) ClosedPopupState else key)
  }
}
