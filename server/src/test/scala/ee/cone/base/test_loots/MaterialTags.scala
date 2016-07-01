package ee.cone.base.test_loots

import ee.cone.base.connection_api.{CoHandlerLists, EventKey}
import ee.cone.base.vdom._
import ee.cone.base.vdom.Types._

//api
sealed trait KeyboardKeyCode
case object EscKeyCode extends KeyboardKeyCode
trait DividerTags {
  def divider(key:VDomKey): ChildPair[OfDiv]
}
case object ToolbarButtons extends EventKey[()⇒List[ChildPair[OfDiv]]]
case object MenuItems extends EventKey[()⇒List[ChildPair[OfDiv]]]
case object NavMenuPopupState extends PopupState

//material
case class Paper() extends VDomValue {
  def appendJson(builder: JsonBuilder) = {
    builder.startObject()
    builder.append("tp").append("Paper") //style?
    builder.end()
  }
}
case class InsetPaper() extends VDomValue{
  def appendJson(builder: JsonBuilder) = {
    builder.startObject()
    builder.append("tp").append("div")
    builder.append("style").startObject()
      builder.append("backgroundColor").append("rgb(255,255,255)")
      builder.append("transition").append("all 450ms cubic-bezier(0.23, 1, 0.32, 1) 0ms")
      builder.append("boxSizing").append("border-box")
      builder.append("fontFamily").append("Roboto,sans-serif")
      builder.append("borderRadius").append("2px")
      builder.append("boxShadow").append("0px 1px 6px rgba(0, 0, 0, 0.12) inset, 0px 1px 4px rgba(0, 0, 0, 0.12) inset")
    builder.end()
    builder.end()
  }
}

case class Divider() extends VDomValue{
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

case class MaterialChip(text:String)(val onClick:Option[()=>Unit]) extends VDomValue with OnClickReceiver{
  def appendJson(builder: JsonBuilder)={
    builder.startObject()
      builder.append("tp").append("MaterialChip")
      builder.append("text").append(text)
      if(onClick.nonEmpty)
        builder.append("onClick").append("send")
    builder.end()
  }
}

case class Helmet(title:String,addViewPort:Boolean) extends VDomValue{
  def appendJson(builder: JsonBuilder)={
    builder.startObject()
    builder.append("tp").append("Helmet")
    builder.append("title").append(title)
    builder.append("style").startArray().startObject()
      builder.append("cssText").append("::-ms-clear {display: none;} ::-ms-reveal {display: none;}")
    builder.end().end()
    if(addViewPort) {
      builder.append("meta").startArray().startObject()
        builder.append("name").append("viewport")
        builder.append("content").append("width=device-width, initial-scale=1")
      builder.end().end()
    }
    builder.end()
  }
}

case class SnackBar(message:String,actionLabel:String,show:Boolean)(val onClick:Option[()=>Unit]) extends VDomValue with OnClickReceiver{
  def appendJson(builder: JsonBuilder)={
    builder.startObject()
    builder.append("tp").append("SnackBarEx")
    //builder.append("onClick").append("send")
    builder.append("open").append(show)
    builder.append("message").append(message)
    if(actionLabel.nonEmpty)
      builder.append("action").append(actionLabel)
    builder.end()
  }
}

case class KeyboardReceiver(keyCode: KeyboardKeyCode)(change:()=>Unit) extends VDomValue with OnChangeReceiver{
  def onChange = Some((code:String) ⇒ change())
  def appendJson(builder: JsonBuilder)={
    builder.startObject()
    builder.append("tp").append("KeyboardReceiver")
    builder.append("keyCode").append(keyCode match {
      case EscKeyCode => "27"
    })
    builder.append("send").append(true)
    builder.append("onChange").append("send")
    builder.end()
  }
}

case class MuiTheme() extends VDomValue{
  def appendJson(builder: JsonBuilder)={
    builder.startObject()
    builder.append("tp").append("MuiThemeParent")
    builder.end()
  }
}

class MaterialTableTags(
  wrapped: TableTags, divTags: DivTags, style: TagStyles
) extends TableTags {
  def table(key: VDomKey, attr: List[TagAttr])(children: List[ChildOfTable]) = wrapped.table(key,attr)(children)
  def row(key: VDomKey, attr: List[TagAttr])(children: List[ChildOfTableRow]) = wrapped.row(key,attr)(children)
  def group(key: VDomKey, attr: TagAttr*) = wrapped.group(key, attr:_*)
  def cell(key: VDomKey, attr: TagAttr*)(children: CellContentVariant ⇒ List[ChildPair[OfDiv]]) =
    wrapped.cell(key, List(style.alignMiddle) ++ attr:_*)(showLabel⇒
      List(divTags.div("1",style.marginSide(10))(children(showLabel)))
    )
}

class MaterialTags(
  handlerLists: CoHandlerLists,
  child: ChildPairFactory, tags: Tags, style: TagStyles, divTags: DivTags,
  popup: Popup, materialIconTags: MaterialIconTags
) extends Tags with DividerTags {
  import divTags._
  import materialIconTags._
  def materialChip(key:VDomKey,text:String)(action:Option[()=>Unit],children:List[ChildPair[OfDiv]]=Nil)= //cust
    child[OfDiv](key,MaterialChip(text)(action),children)
  def inset(key:VDomKey, children: List[ChildPair[OfDiv]]) = //cust
    child[OfDiv](key,InsetPaper(), children)
  def paperWithMargin(key: VDomKey, children: ChildPair[OfDiv]*) = //cust
    div(key, style.margin(10))(List(
      child[OfDiv]("paper", Paper(), List(
        div(key, style.padding(10))(children.toList)
      ))
    ))
  def alert(key:VDomKey, content:String) =
    div(key, style.color(AlertTextColor))(List(text("1",content)))
  def helmet(title:String,addViewPort:Boolean = true)=
    child[OfDiv]("helmet",Helmet(title,addViewPort),Nil)
  def toolbar(title:String): ChildPair[OfDiv] = {
    val popupKey = NavMenuPopupState
    paperWithMargin("toolbar",
      div("1",style.minWidth(200),style.height(50))(List(
        div("1",style.displayTable,style.widthAll,style.heightAll)(List(
          helmet(title),
          div("menu", style.displayCell, style.heightAll, style.alignLeft)(List(
            fieldPopupBox("menu",
              List(btnMenu("menu",()⇒ popup.opened = if(popup.opened == popupKey) ClosedPopupState else popupKey)),
              if(popup.opened != popupKey) Nil else handlerLists.list(MenuItems).flatMap(_())
            )
          )),
          div("title", style.displayCell, style.widthAll, style.heightAll, style.alignLeft, style.alignMiddle, style.paddingSide(10))(List(
            div("1",style.marginSide(10))(List(
              text("1",title)
            ))
          )),
          div("buttons", style.displayCell, style.heightAll, style.alignRight)(
            handlerLists.list(ToolbarButtons).flatMap(_())
          )
        ))
      ))
    )
  }
  def notification(message:String, actionLabel:String = "",show:Boolean=true,close:()=>Unit) =
    child[OfDiv]("notification",SnackBar(message,actionLabel,show)(None),Nil) ::
      (if(show) List(child[OfDiv]("keyboardReceiver", KeyboardReceiver(EscKeyCode)(close), Nil)) else Nil)
  private def muiTheme(theChild:ChildPair[OfDiv]*)=
    child[OfDiv]("muiTheme",MuiTheme(),theChild.toList)
  def root(children: List[ChildPair[OfDiv]]) =
    tags.root(List(muiTheme(div("minWidth320",style.minWidth(320))(children))))
  def text(key: VDomKey, text: String) = tags.text(key,text)
  def divider(key:VDomKey) = child[OfDiv](key,Divider(),Nil)
}
