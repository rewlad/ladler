package ee.cone.base.test_loots

import java.time.{ZoneOffset, LocalDateTime, LocalTime, Instant}

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
    if(color.nonEmpty) {
      builder.append("style").startObject()
        builder.append("fill").append(color.get)
      builder.end()
    }
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
case class InputField[Value](tp: String, value: Value, label: String,deferSend: Boolean,isPassword:Boolean=false)(
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
    if(isPassword) builder.append("type").append("password")
    input.appendJson(builder, convertToString(value), deferSend)
    builder.append("style"); {
      builder.startObject()
      builder.append("width").append("100%")
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
case class CheckBox(checked:Boolean)(
    val onChange: Option[String=>Unit]
) extends VDomValue with OnChangeReceiver {
  def appendJson(builder: JsonBuilder)={
    builder.startObject()
      builder.append("tp").append("Checkbox")
      builder.append("labelPosition").append("left")
      builder.append("onCheck").append("send")
      builder.append("checked").append(checked)
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
case class MaterialChip(text:String) extends VDomValue{

  def appendJson(builder: JsonBuilder)={
    builder.startObject()
      builder.append("tp").append("MaterialChip")
      builder.append("text").append(text)
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
trait Color{
  def color:String
}
case object MenuItemHoverColor extends Color{
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
  def materialChip(key:VDomKey,text:String)=
    child[OfDiv](key,MaterialChip(text),Nil)
  def fieldPopupBox(key: VDomKey,opened:Boolean,chl1:List[ChildPair[OfDiv]],chl2:List[ChildPair[OfDiv]])=
    child[OfDiv](key,DivPositionWrapper(Option("inline-block"),None,Some("relative"),None),List(
      child[OfDiv](key+"box",FieldPopupBox(),chl1),
      child[OfDiv](key+"popup",FieldPopupDrop(opened),chl2))
    )
  def divider(key:VDomKey)=
    child[OfDiv](key,Divider(),Nil)
  def paper(key: VDomKey, children: ChildPair[OfDiv]*) =
    child[OfDiv](key, Paper(), children.toList)
  def checkBox(key:VDomKey,checked:Boolean,check:Boolean=>Unit)=
    child[OfDiv](key,CheckBox(checked)(Some(v⇒check(v.nonEmpty))),Nil)

  def iconInput(key: VDomKey,picture:String,focused:Boolean=false)(theChild:ChildPair[OfDiv]*)=

      divNoWrap("1",
        Seq(
          child[OfDiv]("icon",DivPositionWrapper(Option("inline-block"),Some("36px"),Some("relative"),Some(6)),
            child[OfDiv]("icon",SVGIcon(picture,if(focused) Some("rgb(0,188,212)") else None),Nil)::Nil
          ),
          child[OfDiv]("1",DivPositionWrapper(Option("inline-block"),Some("100%"),None,None),theChild.toList)
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
  def divClickable(key:VDomKey,action:Option[()=>Unit],theChild:ChildPair[OfDiv]*)=
    child[OfDiv](key,DivClickable()(action),theChild.toList)

  def withMaxWidth(key:VDomKey,value:Int,children:List[ChildPair[OfDiv]])=
    child[OfDiv](key,DivMaxWidth(value),children)
  def btnRaised(key: VDomKey, label: String)(action: ()=>Unit) =
    child[OfDiv](key, RaisedButton(label)(Some(action)), Nil)

  def textInput(key: VDomKey, label: String, value: String, change: String⇒Unit, deferSend: Boolean) =
    child[OfDiv](key, InputField("TextField",value, label, deferSend)
      (inputAttributes, identity, Some(newValue ⇒ change(newValue))), Nil)

  def passInput(key: VDomKey, label: String, value: String, change: String⇒Unit, deferSend: Boolean) =
    child[OfDiv](key, InputField("TextField",value, label, deferSend,isPassword = true)
    (inputAttributes, identity, Some(newValue ⇒ change(newValue))), Nil)

  private def instantToString(value: Option[Instant]): String =
    value.map(_.toEpochMilli.toString).getOrElse("")

  private def stringToInstant(value: String): Option[Instant] =
    if(value.nonEmpty) Some(Instant.ofEpochMilli(java.lang.Long.valueOf(value))) else None

  def dateInput(key: VDomKey, label: String, value: Option[Instant], change: Option[Instant]⇒Unit) =
    child[OfDiv](key, InputField("DateInput",value,label, deferSend = false)(inputAttributes,
      instantToString, Some(newValue ⇒ change(stringToInstant(newValue)))), Nil)
  def timeInput(key:VDomKey, label:String, value:Option[Instant], change:Option[Instant]=>Unit)=
    child[OfDiv](key,InputField("TimeInput",value,label, deferSend=false)(inputAttributes,
      instantToString,Some(newValue=>change(stringToInstant(newValue)))),Nil)

  def labeledText(key:VDomKey, label:String, content:String) =
    if(label.nonEmpty) child[OfDiv](key,LabeledTextComponent(content,label),Nil)
    else child[OfDiv](key, TextContentElement(content), Nil)

  def divHeightWrapper(key:VDomKey,height:Int,theChild:ChildPair[OfDiv])=
    child[OfDiv](key,DivHeightWrapper(height),theChild::Nil)
  def divEmpty(key:VDomKey)=
    child[OfDiv](key,DivEmpty(),Nil)
  def divBgColorHover(key:VDomKey,color:Color,theChild:ChildPair[OfDiv]*)=
    child[OfDiv](key,DivBgColorHover(color),theChild.toList)
}

