package ee.cone.base.test_loots

import java.time._

import ee.cone.base.connection_api.DictMessage
import ee.cone.base.db.{AttrValueType, UIStrings}
import ee.cone.base.vdom._
import ee.cone.base.vdom.Types._

sealed trait ValidationKey
case object RequiredValidationKey extends ValidationKey
case object ErrorValidationKey extends ValidationKey
case object DefaultValidationKey extends ValidationKey

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

case class SVGIcon(tp: String,color:Option[String] = None,verticalAlign: Option[VerticalAlign] = None) extends VDomValue {
  def appendJson(builder: JsonBuilder) = {
    builder.startObject()
    builder.append("tp").append(tp) //color str

      builder.append("style").startObject()
    if(color.nonEmpty)
        builder.append("fill").append(color.get)
    verticalAlign.getOrElse("") match{
      case VerticalAlignTop => builder.append("verticalAlign").append("top")
      case VerticalAlignBottom => builder.append("verticalAlign").append("bottom")
      case VerticalAlignMiddle => builder.append("verticalAlign").append("middle")
      case _ =>
    }

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
    //builder.append("overflow").append("hidden")
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

sealed trait FieldType
case object TextFieldType extends FieldType
case object DateFieldType extends FieldType
case object TimeFieldType extends FieldType
case object DecimalFieldType extends FieldType

case class InputField(
  tp: FieldType, label: String, value: String,
  deferSend: Boolean, alignRight: Boolean, fieldValidationState: ValidationKey,
  isPassword: Boolean = false
)(
  input: InputAttributes, val onChange: Option[String=>Unit]
) extends VDomValue with OnChangeReceiver {
  def appendJson(builder: JsonBuilder) = {
    builder.startObject()
    builder.append("tp").append(tp match {
      case TextFieldType ⇒ "TextField"
      case DateFieldType ⇒ "DateInput"
      case TimeFieldType ⇒ "TimeInput"
      case DecimalFieldType => "DecimalInput"
    })
    //builder.append("errorText").append("ehhe")
    if(label.nonEmpty) builder.append("floatingLabelText").append(label)
    (fieldValidationState match {
      case DefaultValidationKey ⇒ ""
      case RequiredValidationKey ⇒ "#ff9800"
      case ErrorValidationKey ⇒ "#f44336"
    }) match {
      case "" ⇒ //"rgba(0,0,0,0.24)"
      case color ⇒
        builder.append("underlineStyle").startObject()
        //builder.append("bottom").append("6px")
        builder.append("borderColor").append(color)
        builder.end()
      //builder.append("hintText").append(fieldValidationState.msg)
    }

    if(isPassword) {
      builder.append("type").append("password")
      builder.append("autoComplete").append("new-password")
    }
    input.appendJson(builder, value, deferSend)
    builder.append("style"); {
      builder.startObject()
      builder.append("width").append("100%")
      builder.append("fontSize").append("13px")
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
case class LabeledTextElement(text:String,label:String) extends VDomValue{
  def appendJson(builder:JsonBuilder)={
    builder.startObject()
      builder.append("tp").append("LabeledText")
      builder.append("content").append(text)
      builder.append("label").append(label)
      builder.append("style").startObject()
      builder.append("fontSize").append("13px")
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
case class FieldPopupBox() extends VDomValue{
  def appendJson(builder: JsonBuilder)={
    builder.startObject()
    builder.append("tp").append("FieldPopupBox")
    builder.append("popupReg").append("def")
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
case class DivZIndex(zIndex:Int) extends VDomValue{
  def appendJson(builder: JsonBuilder)={
    builder.startObject()
    builder.append("tp").append("div")
    builder.append("style").startObject()
      builder.append("position").append("relative")
      builder.append("zIndex").append(s"$zIndex")
    builder.end()
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
case object AlertTextColor extends Color {
  def color="#f44336"
}


  /*
//todo: DateField, SelectField
//todo: Helmet, tap-event, StickyToolbars
//todo: port: import MaterialChip      from './material-chip'   !!!Done

*/

case class CalendarDialog(date:String)(val onChange:Option[(String)=>Unit]) extends VDomValue with OnChangeReceiver{
  def appendJson(builder: JsonBuilder)={
    builder.startObject()
    builder.append("tp").append("CrazyCalendar")
    builder.append("onChange").append("send")
    builder.append("initialDate").append(date)
    builder.end()
  }
}
case class ClockDialog(time:String)(val onChange:Option[(String)=>Unit]) extends VDomValue with OnChangeReceiver{
  def appendJson(builder: JsonBuilder)={
    builder.startObject()
    builder.append("tp").append("CrazyClock")
    builder.append("onChange").append("send")
    builder.append("initialTime").append(time)
    builder.end()
  }
}

case class ColoredTextElement(content: String, color: Color) extends VDomValue {
  def appendJson(builder: JsonBuilder) = {
    builder.startObject()
    builder.append("tp").append("span")
    builder.append("content").append(content);
    {
      builder.append("style").startObject()
      builder.append("color").append(color.color)
      builder.end()
    }
    builder.end()
  }
}

trait OfTable
trait OfTableRow

class MaterialTags(
  child: ChildPairFactory, inputAttributes: InputAttributes, tags: Tags
) {
  def materialChip(key:VDomKey,text:String)(action:Option[()=>Unit],children:List[ChildPair[OfDiv]]=Nil)=
    child[OfDiv](key,MaterialChip(text)(action),children)
  def fieldPopupBox(key: VDomKey,fieldChildren:List[ChildPair[OfDiv]], popupChildren:List[ChildPair[OfDiv]]) =
    child[OfDiv](key,DivPositionWrapper(Option("block"),Some("100%"),Some("relative"),None),List(
      child[OfDiv](key+"box", FieldPopupBox(), fieldChildren),
      child[OfDiv](key+"popup", FieldPopupDrop(popupChildren.nonEmpty,maxHeight = Some(400)), popupChildren)
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

  def iconInput(key: VDomKey,picture:String,focused:Boolean=false)(input: List[ChildPair[OfDiv]])=
      divNoWrap("1",
        Seq(
          child[OfDiv]("icon",DivPositionWrapper(Option("inline-block"),Some("36px"),Some("relative"),None),
            child[OfDiv]("icon",SVGIcon(picture,if(focused) Some("rgb(0,188,212)") else None),Nil)::Nil
          ),
          child[OfDiv]("1",DivPositionWrapper(Option("inline-block"),Some("calc(100% - 36px)"),None,None), input)
        ):_*
      )

  def btnInput(key: VDomKey,zIndex : Int)(btn:ChildPair[OfDiv],input:ChildPair[OfDiv])=
    divNoWrap("1",
      List(
        child[OfDiv]("1",DivPositionWrapper(Option("inline-block"),Some("calc(100% - 48px)"),None,None),input::Nil),
        child[OfDiv]("icon",DivPositionWrapper(Option("inline-block"),Some("48px"),None,None),
          withZIndex("zIndex",zIndex,btn)::Nil
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
  def calendarDialog(key:VDomKey,date:String,action: Option[(String)=>Unit])={
    child[OfDiv](key,CalendarDialog(date)(action),Nil)
  }
  def clockDialog(key: VDomKey,time:String,action: Option[(String)=>Unit])={
    child[OfDiv](key,ClockDialog(time)(action),Nil)
  }
  def iconArrowUp()=
    child[OfDiv]("icon",SVGIcon("IconNavigationDropDown",verticalAlign = Some(VerticalAlignMiddle)),Nil)
  def iconArrowDown()=
    child[OfDiv]("icon",SVGIcon("IconNavigationDropUp",verticalAlign = Some(VerticalAlignMiddle)),Nil)
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
    iconButton(key,"add","IconContentAdd",action)
  def btnRemove(key: VDomKey, action: ()=>Unit) =
    iconButton(key,"remove","IconContentRemove",action)
  def btnModeEdit(key:VDomKey, action:()=>Unit) =
    iconButton(key,"edit","IconEditorModeEdit",action)
  def btnDelete(key:VDomKey,action:()=>Unit) =
    iconButton(key,"delete","IconActionDelete",action)
  def btnMenu(key:VDomKey,action:()=>Unit) =
    iconButton(key,"menu","IconNavigationMenu",action)
  def btnDateRange(key:VDomKey,action:()=>Unit) =
    iconButton(key,"calendar","IconActionDateRange",action)
  def btnExpandMore(key:VDomKey,action:()=>Unit) =
    iconButton(key,"more","IconNavigationExpandMore",action)
  def btnExpandLess(key:VDomKey,action:()=>Unit) =
    iconButton(key,"less","IconNavigationExpandLess",action)
  def btnScheduleClock(key:VDomKey,action:()=>Unit) =
    iconButton(key,"clock","IconActionSchedule",action)
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
  def inputField(
      key: VDomKey,
      inputType: FieldType, label: String, value: String,
      change: String⇒Unit,
      deferSend: Boolean, alignRight: Boolean, fieldValidationState: ValidationKey,
      isPassword: Boolean = false
  ) = {
    val input = InputField(
      inputType, label, value,
      deferSend, alignRight, fieldValidationState,
      isPassword
    )(inputAttributes, Some(change))
    child[OfDiv](key, input, Nil)
  }

  def alert(key:VDomKey, content:String) =
    child[OfDiv](key, ColoredTextElement(content,AlertTextColor), Nil)

  def labeledText(key:VDomKey, label:String, content:String) =
    if(label.nonEmpty) child[OfDiv](key,LabeledTextElement(content,label),Nil)
    else tags.text(key, content)

  def divHeightWrapper(key:VDomKey,height:Int,theChild:ChildPair[OfDiv]*)=
    child[OfDiv](key,DivHeightWrapper(height),theChild.toList)
  def divEmpty(key:VDomKey)=
    child[OfDiv](key,DivEmpty(),Nil)
  def divBgColorHover(key:VDomKey,color:Color,theChild:ChildPair[OfDiv]*)=
    child[OfDiv](key,DivBgColorHover(color),theChild.toList)
  def withBgColor(key:VDomKey,color:Color,theChild:ChildPair[OfDiv]*)=
    child[OfDiv](key,DivBgColor(color),theChild.toList)
  def withZIndex(key:VDomKey,zIndex:Int,theChild:ChildPair[OfDiv]*)=
    child[OfDiv](key,DivZIndex(zIndex),theChild.toList)

}

