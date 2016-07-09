
package ee.cone.base.material_impl

// to mat

import ee.cone.base.connection_api.{CoHandlerLists, EventKey}
import ee.cone.base.material.{ToolbarButtons, MenuItems, MaterialTags,
ButtonTags}
import ee.cone.base.vdom._
import ee.cone.base.vdom.Types._

//material
sealed trait KeyboardKeyCode
case object EscKeyCode extends KeyboardKeyCode

case object NavMenuPopupState extends PopupState

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

case object AlertTextColor extends Color { def value="#f44336" }

case object IconNavigationMenu extends TagName("IconNavigationMenu")

class MaterialTagsImpl(
  handlerLists: CoHandlerLists, dbRootWrap: DBRootWrap,
  child: ChildPairFactory, tags: Tags, style: TagStyles,
  optionTags: OptionTagsI, buttonTags: ButtonTags, materialStyles: MaterialStyles
) extends MaterialTags {
  import tags._
  import optionTags._
  import buttonTags._
  import materialStyles._

  def materialChip(key:VDomKey,text:String)(action:Option[()=>Unit],children:List[ChildPair[OfDiv]]=Nil) = //cust
    child[OfDiv](key,MaterialChip(text)(action),children)
  def inset(key:VDomKey, children: List[ChildPair[OfDiv]]) = //cust
    child[OfDiv](key,InsetPaper(), children)
  def paperWithMargin(key: VDomKey, children: ChildPair[OfDiv]*) = //cust
    div(key, style.margin(10))(List(
      child[OfDiv]("paper", Paper(), List(
        div(key, style.padding(10))(children.toList)
      ))
    ))
  def alert(key:VDomKey, content:String) = //cust
    div(key, style.color(AlertTextColor))(List(text("1",content)))


  def helmet(title:String,addViewPort:Boolean = true) = //cust
    child[OfDiv]("helmet",Helmet(title,addViewPort),Nil)
  def toolbar(title:String): ChildPair[OfDiv] = { //cust
    val (isOpened, toggle) = popupAction(NavMenuPopupState)
    paperWithMargin("toolbar",
      div("1",style.minWidth(200),style.height(50))(List(
        div("1",style.displayTable,style.widthAll,style.heightAll)(List(
          helmet(title),
          div("menu", style.displayCell, style.heightAll, style.alignLeft)(List(
            popupBox("menu",
              List(iconButton("menu","menu",IconNavigationMenu)(toggle)),
              if(!isOpened) Nil else handlerLists.list(MenuItems).flatMap(_())
            )
          )),
          div("title", style.displayCell, style.widthAll, style.heightAll, style.alignLeft, style.alignMiddle, paddingSide(10))(List(
            div("1",marginSide)(List(
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
  def wrap(children: ()⇒List[ChildPair[OfDiv]]) =
    List(muiTheme(div("minWidth320",style.minWidth(320))(
      dbRootWrap.wrap(()⇒children())
    )))
}
