package ee.cone.base.test_loots

import java.time._

import ee.cone.base.connection_api.DictMessage
import ee.cone.base.vdom._
import ee.cone.base.vdom.Types._

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

case class Table() extends VDomValue {
  def appendJson(builder: JsonBuilder) = {
    builder.startObject()
    builder.append("tp").append("table")
    builder.append("style"); {
      builder.startObject()
      builder.append("borderCollapse").append("collapse")
      builder.end()
    }
    /*
    builder.append("tp").append("Table")
    //builder.append("fixedHeader").append(false)
    builder.append("displayRowCheckbox").append(false) //try
    */
    builder.end()
  }
}

case class TableHeader() extends VDomValue {
  def appendJson(builder: JsonBuilder) = {
    builder.startObject()
    builder.append("tp").append("thead")
    /*
    builder.append("tp").append("TableHeader")
    builder.append("adjustForCheckbox").append(true) //for indent
    builder.append("displaySelectAll").append(false)
    */
    builder.end()
  }
}

case class TableBody() extends VDomValue {
  def appendJson(builder: JsonBuilder) = {
    builder.startObject()
    builder.append("tp").append("tbody")
    /*
    builder.append("tp").append("TableBody")
    builder.append("displayRowCheckbox").append(true) //try
    */
    builder.end()
  }
}

case class TableRow() extends VDomValue {
  def appendJson(builder: JsonBuilder) = {
    builder.startObject()
    //builder.append("tp").append("TableRow") // selectable -- checkbox enabled
    builder.append("tp").append("tr")
    builder.end()
  }
}

case class TableColumn(isHead: Boolean, isRight: Boolean, colSpan: Int, isUnderline: Boolean)(
    val onClick: Option[()⇒Unit]
) extends VDomValue with OnClickReceiver {
  def appendJson(builder: JsonBuilder) = {
    builder.startObject()
    //builder.append("tp").append(if(isHead) "TableHeaderColumn" else "TableRowColumn")
    builder.append("tp").append(if(isHead) "th" else "td")
    onClick.foreach(_⇒ builder.append("onClick").append("send"))
    if(colSpan != 1) builder.append("colSpan").append(colSpan.toString) //may be append int or transform
    builder.append("style"); {
      builder.startObject()
      builder.append("textAlign").append(if(isRight) "right" else "left")
      if(isUnderline) builder.append("borderBottom").append("1px solid silver")
      builder.end()
    }
    builder.end()
  }
}

case class IconButton(tooltip: String)(
    val onClick: Option[()⇒Unit]
) extends VDomValue with OnClickReceiver {
  def appendJson(builder: JsonBuilder) = {
    builder.startObject()
    builder.append("tp").append("IconButton")
    if(tooltip.nonEmpty) builder.append("tooltip").append(tooltip) //todo: color str
    onClick.foreach(_⇒ builder.append("onClick").append("send"))
    builder.end()
  }
}

case class SVGIcon(tp: String,color:Option[String] = None) extends VDomValue {
  def appendJson(builder: JsonBuilder) = {
    builder.startObject()
    builder.append("tp").append(tp) //color str

      builder.append("style").startObject()
    if(color.nonEmpty)
        builder.append("fill").append(color.get)
        builder.append("verticalAlign").append("middle")
      builder.end()
    builder.end()
  }
}

case class MarginWrapper(value: Int,inline:Boolean=true) extends VDomValue {
  def appendJson(builder: JsonBuilder) = {
    builder.startObject()
    if(inline)
      builder.append("tp").append("span")
    else
      builder.append("tp").append("div")
    builder.append("style"); {
      builder.startObject()
      builder.append("margin").append(s"${value}px")
      builder.end()
    }
    builder.end()
  }
}
case class MarginSideWrapper(value:Int) extends VDomValue{
  def appendJson(builder: JsonBuilder)={
    builder.startObject()
      builder.append("tp").append("div")
      builder.append("style").startObject()
        builder.append("marginLeft").append(s"${value}px")
        builder.append("marginRight").append(s"${value}px")
        //builder.append("height").append("100%")
      builder.end()
    builder.end()
  }
}
case class DivMaxWidth(value:Int) extends VDomValue{
  def appendJson(builder: JsonBuilder)={
    builder.startObject()
      builder.append("tp").append("div")
      builder.append("style").startObject()
        builder.append("maxWidth").append(s"${value}px")
        builder.append("margin").append("0px auto")
      builder.end()
    builder.end()
  }
}
case class DivMinWidth(value:Int) extends VDomValue{
  def appendJson(builder: JsonBuilder)={
    builder.startObject()
    builder.append("tp").append("div")
    builder.append("style").startObject()
    builder.append("minWidth").append(s"${value}px")
    builder.end()
    builder.end()
  }
}
case class DivMinHeight(value:Int) extends VDomValue{
  def appendJson(builder: JsonBuilder)={
    builder.startObject()
    builder.append("tp").append("div")
    builder.append("style").startObject()
    builder.append("minHeight").append(s"${value}px")
    builder.end()
    builder.end()
  }
}
case class PaddingWrapper(value: Int) extends VDomValue {
  def appendJson(builder: JsonBuilder) = {
    builder.startObject()
    builder.append("tp").append("div")
    builder.append("style"); {
      builder.startObject()
      builder.append("padding").append(s"${value}px")
      builder.end()
    }
    builder.end()
  }
}

case class PaddingSideWrapper(value:Int) extends VDomValue{
  def appendJson(builder: JsonBuilder)={
    builder.startObject()
      builder.append("tp").append("div")
      builder.append("style").startObject()
        builder.append("paddingLeft").append(s"${value}px")
        builder.append("paddingRight").append(s"${value}px")
        builder.append("height").append("100%")
      builder.end()
    builder.end()
  }
}
case class DivMaxHeightWrapper() extends VDomValue{
  def appendJson(builder:JsonBuilder)={
    builder.startObject()
      builder.append("tp").append("div")
      builder.append("style").startObject()
        builder.append("height").append("100%")
      builder.end()
    builder.end()
  }
}
case class DivSimpleWrapper() extends VDomValue{
  def appendJson(builder: JsonBuilder)={
    builder.startObject()
    builder.append("tp").append("div")
    builder.end()
  }
}

case class DivOverflowHidden() extends VDomValue{
  def appendJson(builder: JsonBuilder)={
    builder.startObject()
    builder.append("tp").append("div")
    builder.append("style").startObject()
    builder.append("overflow").append("hidden")
    builder.end()
    builder.end()

  }
}

case class DivNoTextWrap() extends VDomValue{
  def appendJson(builder: JsonBuilder)={
    builder.startObject()
    builder.append("tp").append("div")
    builder.append("style").startObject()
    builder.append("whiteSpace").append("nowrap")
    builder.append("overflow").append("hidden")
    builder.end()
    builder.end()

  }
}
case class DivClickable()(val onClick:Option[()=>Unit]) extends VDomValue with OnClickReceiver{
  def appendJson(builder: JsonBuilder)={
    builder.startObject()
    builder.append("tp").append("div")
    onClick.foreach(_⇒ builder.append("onClick").append("send"))
    builder.append("style").startObject()
      builder.append("cursor").append("pointer")
    builder.end()
    builder.end()

  }
}

case class DivHeightWrapper(height:Int) extends VDomValue{
  def appendJson(builder: JsonBuilder)={
    builder.startObject()
    builder.append("tp").append("div")
    builder.append("style").startObject()
      builder.append("height").append(s"${height}px")
    builder.end()
    builder.end()
  }
}
case class DivEmpty() extends VDomValue{
  def appendJson(builder: JsonBuilder)={
    builder.startObject()
      builder.append("tp").append("div")
    builder.end()
  }
}
case class InputField[Value](tp: String, value: Value, alignRight:Boolean,label: String,deferSend: Boolean,isPassword:Boolean=false)(
  input: InputAttributes, convertToString: Value⇒String, val onChange: Option[String⇒Unit]
) extends VDomValue with OnChangeReceiver {
  def appendJson(builder: JsonBuilder) = {
    builder.startObject()
    builder.append("tp").append(tp)
    //builder.append("errorText").append("ehhe")
    if(label.nonEmpty) builder.append("floatingLabelText").append(label)
    builder.append("underlineStyle").startObject()
      builder.append("borderColor").append("rgba(0,0,0,0.24)")
    builder.end()
    if(isPassword) {
      builder.append("type").append("password")
      builder.append("autoComplete").append("new-password")
    }
    input.appendJson(builder, convertToString(value), deferSend)
    builder.append("style"); {
      builder.startObject()
      builder.append("width").append("100%")
      builder.end()
    }
    if(alignRight) {
      builder.append("inputStyle").startObject()
        builder.append("textAlign").append("right")
      builder.end()
    }
    builder.end()
  }
}

case class RaisedButton(label: String)(
    val onClick: Option[()⇒Unit]
) extends VDomValue with OnClickReceiver {
  def appendJson(builder: JsonBuilder) = {
    builder.startObject()
    builder.append("tp").append("RaisedButton")
    builder.append("secondary").append(true)
    builder.append("label").append(label)
    onClick.foreach(_⇒ builder.append("onClick").append("send")) //try
    builder.end()
  }
}
case class Divider() extends VDomValue{
  def appendJson(builder: JsonBuilder)={
    builder.startObject()
      builder.append("tp").append("hr")
      builder.append("style").startObject()
        builder.append("didFlip").append(true)
        builder.append("margin").append("0")
        builder.append("marginTop").append("0px")
        builder.append("marginLeft").append("0")
        builder.append("height").append("1px")
        builder.append("border").append("none")
        builder.append("borderBottom").append("1px solid #e0e0e0")
      builder.end()
    builder.end()
  }
}
case class CheckBox(checked:Boolean,label:String)(
    val onChange: Option[String=>Unit]
) extends VDomValue with OnChangeReceiver {
  def appendJson(builder: JsonBuilder)={
    builder.startObject()
      builder.append("tp").append("Checkbox")
      builder.append("labelPosition").append("left")
      builder.append("onCheck").append("send")
      builder.append("checked").append(checked)
      builder.append("label").append(label)
      builder.append("labelStyle").startObject()
      builder.append("fontSize").append("12")
      builder.append("color").append("rgba(0,0,0,0.3)")
      builder.end()
    builder.end()
  }
}
case class LabeledTextComponent(text:String,label:String) extends VDomValue{

  def appendJson(builder:JsonBuilder)={
    builder.startObject()
      builder.append("tp").append("LabeledText")
      builder.append("content").append(text)
      builder.append("label").append(label)
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
/*
case class SelectField(label:String,showDrop:Boolean)(onFocus:(Boolean)=>()=>Unit) extends VDomValue with
  OnBlurReceiver with OnClickReceiver with OnChangeReceiver{

  def onClick()=Option(onFocus(true))
  def onBlur=Option(onFocus(false))
  def onChange=Option((x)=>{println(x+"change")})
  def appendJson(builder: JsonBuilder)={
    builder.startObject()
      builder.append("tp").append("SelectField")
      builder.append("onChange").append("send")
      builder.append("onBlur").append("blur")
      builder.append("onClick").append("send")
      builder.append("showDrop").append(showDrop)
      builder.append("label").append(label)
    builder.end()

  }
}
*/
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
case class FieldPopupBox(showUnderscore:Boolean) extends VDomValue{
  def appendJson(builder: JsonBuilder)={
    builder.startObject()
    builder.append("tp").append("FieldPopupBox")
    builder.append("popupReg").append("def")
    builder.append("showUnderscore").append(showUnderscore)
    builder.end()

  }
}
case class IconMenu(opened:Boolean)(val onClick:Option[()=>Unit]) extends VDomValue with OnClickReceiver{
  def appendJson(builder: JsonBuilder)={
    builder.startObject()
    builder.append("tp").append("IconMenuButton")
    builder.append("open").append(opened)
    builder.append("onRequestChange").append("reqChange")
    onClick.foreach(_⇒ builder.append("onClick").append("send"))
    builder.end()
  }
}
case class MenuItem(key:VDomKey,text:String)(val onClick:Option[()=>Unit]) extends VDomValue with OnClickReceiver{
  def appendJson(builder: JsonBuilder)={
    builder.startObject()
    builder.append("tp").append("MenuItem")
    builder.append("primaryText").append(text)
    builder.append("value").append(key.toString)
    onClick.foreach(_⇒ builder.append("onClick").append("send"))
    builder.end()
  }

}

case class DivPositionWrapper(display:Option[String],
                              width:Option[String],
                              position:Option[String],
                              top:Option[Int]) extends VDomValue{
  def appendJson(builder: JsonBuilder)={
    builder.startObject()
    builder.append("tp").append("div")
    builder.append("style").startObject()
    display.foreach(x=>builder.append("display").append(x))
    position.foreach(x=>builder.append("position").append(x))
    top.foreach(x=>builder.append("top").append(s"${x}px"))
    if(width.nonEmpty) builder.append("width").append(width.get)
    builder.end()
    builder.end()
  }
}
case class DivBgColorHover(color:Color) extends VDomValue{
  def appendJson(builder: JsonBuilder)={
    builder.startObject()
    builder.append("tp").append("CursorOver")
    builder.append("hoverColor").append(color.color)
    builder.end()
  }
}
case class DivBgColor(color:Color) extends VDomValue{
  def appendJson(builder: JsonBuilder)={
    builder.startObject()
    builder.append("tp").append("div")
    builder.append("style").startObject()
    builder.append("backgroundColor").append(color.color)
    builder.end()
    builder.end()
  }
}
trait Color{
  def color:String
}
case object MenuItemHoverColor extends Color{
  def color="rgba(0,0,0,0.1)"
}
case object ControlPanelColor extends Color{
  def color="rgba(0,0,0,0.1)"
}

  /*
//todo: DateField, SelectField
//todo: Helmet, tap-event, StickyToolbars
//todo: port: import MaterialChip      from './material-chip'   !!!Done

*/

trait OfTable
trait OfTableRow

class MaterialTags(
  child: ChildPairFactory, inputAttributes: InputAttributes
) {
  def materialChip(key:VDomKey,text:String)(action:Option[()=>Unit],children:List[ChildPair[OfDiv]]=Nil)=
    child[OfDiv](key,MaterialChip(text)(action),children)
  def fieldPopupBox(key: VDomKey, chl1:List[ChildPair[OfDiv]], chl2:List[ChildPair[OfDiv]]) =
    child[OfDiv](key,DivPositionWrapper(Option("inline-block"),None,Some("relative"),None),List(
      child[OfDiv](key+"box", FieldPopupBox(showUnderscore = false), chl1),
      child[OfDiv](key+"popup", FieldPopupDrop(chl2.nonEmpty), chl2)
    ))
  def fieldPopupBox(key: VDomKey, showUnderscore:Boolean,chl1:List[ChildPair[OfDiv]], chl2:List[ChildPair[OfDiv]]) =
    child[OfDiv](key,DivPositionWrapper(Option("inline-block"),Some("100%"),Some("relative"),None),List(
      child[OfDiv](key+"box", FieldPopupBox(showUnderscore), chl1),
      child[OfDiv](key+"popup", FieldPopupDrop(chl2.nonEmpty), chl2)
    ))
  def divider(key:VDomKey)=
    child[OfDiv](key,Divider(),Nil)
  def paper(key: VDomKey, inset: Boolean=false)(children: ChildPair[OfDiv]*) =
    if(inset)
      child[OfDiv](key,InsetPaper(),children.toList)
    else
    child[OfDiv](key, Paper(), children.toList)

  def checkBox(key:VDomKey,label:String,checked:Boolean,check:Boolean=>Unit)=
    child[OfDiv](key,CheckBox(checked,label)(Some(v⇒check(v.nonEmpty))),Nil)

  def iconInput(key: VDomKey,picture:String,focused:Boolean=false)(children: List[ChildPair[OfDiv]])=
      divNoWrap("1",
        Seq(
          child[OfDiv]("icon",DivPositionWrapper(Option("inline-block"),Some("36px"),Some("relative"),None),
            child[OfDiv]("icon",SVGIcon(picture,if(focused) Some("rgb(0,188,212)") else None),Nil)::Nil
          ),
          child[OfDiv]("1",DivPositionWrapper(Option("inline-block"),Some("calc(100% - 36px)"),None,None), children)
        ):_*
      )

  def btnInput(key: VDomKey)(btn:ChildPair[OfDiv],input:ChildPair[OfDiv])=
    divNoWrap("1",
      List(
        child[OfDiv]("1",DivPositionWrapper(Option("inline-block"),Some("calc(100% - 48px)"),None,None),input::Nil),
        child[OfDiv]("icon",DivPositionWrapper(Option("inline-block"),Some("48px"),None,None),
          btn::Nil
        )
      ):_*
    )
  /*
  def table(key: VDomKey, head: List[ChildPair[OfTable]], body: List[ChildPair[OfTable]]) =
    child[OfDiv](key, Table(),
      child("head",TableHeader(), head) ::
      child("body",TableBody(), body) :: Nil
    )
  def row(key: VDomKey, children: ChildPair[OfTableRow]*) =
    child[OfTable](key, TableRow(), children.toList)
  def cell(key: VDomKey, isHead: Boolean=false, isRight: Boolean=false, colSpan: Int=1, isUnderline: Boolean=false)(children: List[ChildPair[OfDiv]]=Nil, action: Option[()⇒Unit]=None) =
    child[OfTableRow](key, TableColumn(isHead, isRight, colSpan, isUnderline)(action), children)
*/
  def iconMenu(key: VDomKey,opened:Boolean)(action:()=>Unit,theChild:ChildPair[OfDiv]*)=
    child[OfDiv](key,IconMenu(opened)(Some(action)),theChild.toList)
  def menuItem(key: VDomKey,text:String)(action:()=>Unit)=
    child[OfDiv](key,MenuItem(key,text)(Some(action)),Nil)
  private def iconButton(key: VDomKey, tooltip: String, picture: String, action: ()=>Unit) =
    child[OfDiv](key,
      IconButton(tooltip)(Some(action)),
      child("icon", SVGIcon(picture), Nil) :: Nil
    )
  def iconArrowUp()=
    child[OfDiv]("icon",SVGIcon("IconNavigationDropDown"),Nil)
  def iconArrowDown()=
    child[OfDiv]("icon",SVGIcon("IconNavigationDropUp"),Nil)
  //def btnViewList(key:VDomKey, action: ()=>Unit) =
  //  iconButton(key,"view list","IconActionViewList",action)
  def btnSave(key:VDomKey, action: ()=>Unit) =
    iconButton(key,"save","IconContentSave",action)
  def btnRestore(key:VDomKey, action: ()=>Unit) =
    iconButton(key,"restore","IconActionRestore",action)
  def btnFilterList(key: VDomKey, action: ()=>Unit) =
    iconButton(key,"filters","IconContentFilterList",action)
  def btnClear(key: VDomKey, action: ()=>Unit) =
    iconButton(key,"clear sorting","IconContentClear",action)
  def btnAdd(key: VDomKey, action: ()=>Unit) =
    iconButton(key,"","IconContentAdd",action)
  def btnRemove(key: VDomKey, action: ()=>Unit) =
    iconButton(key,"","IconContentRemove",action)
  def btnCreate(key:VDomKey,action:()=>Unit)=
    iconButton(key,"","IconContentCreate",action)
  def btnDelete(key:VDomKey,action:()=>Unit)=
    iconButton(key,"","IconActionDelete",action)
  def btnMenu(key:VDomKey,action:()=>Unit)=
    iconButton(key,"menu","IconNavigationMenu",action)
  def btnDateRange(key:VDomKey,action:()=>Unit)=
    iconButton(key,"calendar","IconActionDateRange",action)
  def btnExpandMore(key:VDomKey,action:()=>Unit)=
    iconButton(key,"more","IconNavigationExpandMore",action)
  def btnExpandLess(key:VDomKey,action:()=>Unit)=
    iconButton(key,"less","IconNavigationExpandLess",action)
  def withMargin(key: VDomKey, value: Int, theChild: ChildPair[OfDiv]) =
    child[OfDiv](key, MarginWrapper(value), theChild :: Nil)
  def withMargin(key: VDomKey, value: Int, children: List[ChildPair[OfDiv]]) =
    child[OfDiv](key, MarginWrapper(value), children)
  def withDivMargin(key: VDomKey, value: Int, theChild: ChildPair[OfDiv]*) =
    child[OfDiv](key, MarginWrapper(value,inline = false), theChild.toList)
  def withSideMargin(key:VDomKey,value:Int,theChild:ChildPair[OfDiv])=
    child[OfDiv](key,MarginSideWrapper(value),theChild::Nil)
  def withSideMargin(key:VDomKey,value:Int,children:List[ChildPair[OfDiv]])=
    child[OfDiv](key,MarginSideWrapper(value),children)
  def withPadding(key: VDomKey, value: Int, theChild: ChildPair[OfDiv]*) =
    child[OfDiv](key, PaddingWrapper(value), theChild.toList)
  def withSidePadding(key: VDomKey,value:Int,theChild:ChildPair[OfDiv])=
    child[OfDiv](key,PaddingSideWrapper(value),theChild::Nil)
  def withSidePadding(key: VDomKey,value:Int,children:List[ChildPair[OfDiv]])=
    child[OfDiv](key,PaddingSideWrapper(value),children)
  def divEWrapper(key:VDomKey,children:List[ChildPair[OfDiv]])=
    child[OfDiv](key,DivMaxHeightWrapper(),children)
  def divEWrapper(key:VDomKey,theChild:ChildPair[OfDiv])=
    child[OfDiv](key,DivMaxHeightWrapper(),theChild::Nil)
  def divSimpleWrapper(key:VDomKey,theChild:ChildPair[OfDiv]*)=
    child[OfDiv](key,DivSimpleWrapper(),theChild.toList)
  def divNoWrap(key:VDomKey,theChild:ChildPair[OfDiv]*)=
    child[OfDiv](key,DivNoTextWrap(),theChild.toList)
  def divOverflowHidden(key:VDomKey,theChild:ChildPair[OfDiv]*)=
    child[OfDiv](key,DivOverflowHidden(),theChild.toList)
  def divClickable(key:VDomKey,action:Option[()=>Unit],theChild:ChildPair[OfDiv]*)=
    child[OfDiv](key,DivClickable()(action),theChild.toList)

  def withMaxWidth(key:VDomKey,value:Int,children:List[ChildPair[OfDiv]])=
    child[OfDiv](key,DivMaxWidth(value),children)
  def withMinWidth(key:VDomKey,value:Int,children:List[ChildPair[OfDiv]])=
    child[OfDiv](key,DivMinWidth(value),children)
  def withMinHeight(key:VDomKey,value:Int,theChild:ChildPair[OfDiv]*)=
    child[OfDiv](key,DivMinHeight(value),theChild.toList)
  def btnRaised(key: VDomKey, label: String)(action: ()=>Unit) =
    child[OfDiv](key, RaisedButton(label)(Some(action)), Nil)

  def textInput(key: VDomKey, label: String, value: String, change: String⇒Unit, deferSend: Boolean, alignRight: Boolean) =
    child[OfDiv](key, InputField("TextField", value, alignRight, label, deferSend, isPassword = false)
      (inputAttributes, identity, Some(newValue ⇒ change(newValue))), Nil)

  def passInput(key: VDomKey, label: String, value: String, change: String⇒Unit, deferSend: Boolean) =
    child[OfDiv](key, InputField("TextField",value, alignRight = false, label, deferSend, isPassword = true)
    (inputAttributes, identity, Some(newValue ⇒ change(newValue))), Nil)

  private def instantToString(value: Option[Instant]): String =
    value.map(_.toEpochMilli.toString).getOrElse("")

  private def stringToInstant(value: String): Option[Instant] =
    if(value.nonEmpty) Some(Instant.ofEpochMilli(java.lang.Long.valueOf(value))) else None

  private def localTimeToString(value: Option[LocalTime]):String=
    value.map(x=>x.getHour().toString+":"+x.getMinute().toString).getOrElse("")

  private def stringToLocalTime(value:String): Option[LocalTime]=
    if(value.nonEmpty) Some(LocalTime.parse(value)) else None

  private def durationToString(value: Option[Duration]):String=

    value.map(x=>{

      val h=if(x.abs.toHours<10) "0"+x.abs.toHours else x.abs.toHours
      val m=if(x.abs.minusHours(x.abs.toHours).toMinutes<10) "0"+x.abs.minusHours(x.abs.toHours).toMinutes else x.abs.minusHours(x.abs.toHours).toMinutes
      h+":"+m
    }).getOrElse("")

  private def stringToDuration(value:String):Option[Duration]=
    if(value.nonEmpty) {
      val hm=value.split(":")
      val h=hm(0)
      val m=hm(1)

      Some(Duration.ofMinutes(h.toLong*60+m.toLong))

    } else None

  def dateInput(key: VDomKey, label: String, value: Option[Instant], change: Option[Instant]⇒Unit) =
    child[OfDiv](key, InputField("DateInput",value,alignRight = false,label, deferSend = false,isPassword = false)(inputAttributes,
      instantToString, Some(newValue ⇒ change(stringToInstant(newValue)))), Nil)
  def localTimeInput(key:VDomKey, label:String, value:Option[LocalTime], change:Option[LocalTime]=>Unit)=
    child[OfDiv](key,InputField("TimeInput",value,alignRight = false,label, deferSend=false,isPassword = false)(inputAttributes,
      localTimeToString,Some(newValue=>change(stringToLocalTime(newValue)))),Nil)
  def durationInput(key:VDomKey, label:String, value:Option[Duration], change:Option[Duration]=>Unit)=
    child[OfDiv](key,InputField("TimeInput",value,alignRight = false,label,deferSend=false,isPassword = false)(inputAttributes,
      durationToString,Some(newValue=>change(stringToDuration(newValue)))),Nil)
  def labeledText(key:VDomKey, label:String, content:String) =
    if(label.nonEmpty) child[OfDiv](key,LabeledTextComponent(content,label),Nil)
    else child[OfDiv](key, TextContentElement(content), Nil)

  def divHeightWrapper(key:VDomKey,height:Int,theChild:ChildPair[OfDiv]*)=
    child[OfDiv](key,DivHeightWrapper(height),theChild.toList)
  def divEmpty(key:VDomKey)=
    child[OfDiv](key,DivEmpty(),Nil)
  def divBgColorHover(key:VDomKey,color:Color,theChild:ChildPair[OfDiv]*)=
    child[OfDiv](key,DivBgColorHover(color),theChild.toList)
  def withBgColor(key:VDomKey,color:Color,theChild:ChildPair[OfDiv]*)=
    child[OfDiv](key,DivBgColor(color),theChild.toList)
}

