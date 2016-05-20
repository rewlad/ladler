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

case class SVGIcon(tp: String) extends VDomValue {
  def appendJson(builder: JsonBuilder) = {
    builder.startObject()
    builder.append("tp").append(tp) //color str
    builder.end()
  }
}

case class MarginWrapper(value: Int) extends VDomValue {
  def appendJson(builder: JsonBuilder) = {
    builder.startObject()
    builder.append("tp").append("span")
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
    builder.end()
    builder.end()

  }
}
case class DivClickable()(val onClick:Option[()=>Unit]) extends VDomValue with OnClickReceiver{
  def appendJson(builder: JsonBuilder)={
    builder.startObject()
    builder.append("tp").append("div")
    builder.append("onClick").append("send")
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
case class InputField[Value](tp: String, value: Value, label: String,showLabel:Boolean,deferSend: Boolean)(
  input: InputAttributes, convertToString: Value⇒String, val onChange: Option[String⇒Unit]
) extends VDomValue with OnChangeReceiver {
  def appendJson(builder: JsonBuilder) = {
    builder.startObject()
    builder.append("tp").append(tp)
    //builder.append("errorText").append("ehhe")
    if(showLabel) builder.append("floatingLabelText").append(label)
    builder.append("underlineStyle").startObject()
      builder.append("borderColor").append("rgba(0,0,0,0.24)")
    builder.end()
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
    divSimpleWrapper(key,
      child[OfDiv](key+"box",FieldPopupBox(),chl1),
      child[OfDiv](key+"popup",FieldPopupDrop(opened),chl2)
    )
  def divider(key:VDomKey)=
    child[OfDiv](key,Divider(),Nil)
  def paper(key: VDomKey, children: ChildPair[OfDiv]*) =
    child[OfDiv](key, Paper(), children.toList)
  def checkBox(key:VDomKey,checked:Boolean=false,check:Boolean=>Unit)=
    child[OfDiv](key,CheckBox(checked)(Some(v⇒check(v.nonEmpty))),Nil)
  def table(key: VDomKey, head: List[ChildPair[OfTable]], body: List[ChildPair[OfTable]]) =
    child[OfDiv](key, Table(),
      child("head",TableHeader(), head) ::
      child("body",TableBody(), body) :: Nil
    )
  def row(key: VDomKey, children: ChildPair[OfTableRow]*) =
    child[OfTable](key, TableRow(), children.toList)
  def cell(key: VDomKey, isHead: Boolean=false, isRight: Boolean=false, colSpan: Int=1, isUnderline: Boolean=false)(children: List[ChildPair[OfDiv]]=Nil, action: Option[()⇒Unit]=None) =
    child[OfTableRow](key, TableColumn(isHead, isRight, colSpan, isUnderline)(action), children)

  private def iconButton(key: VDomKey, tooltip: String, picture: String, action: ()=>Unit) =
    child[OfDiv](key,
      IconButton(tooltip)(Some(action)),
      child("icon", SVGIcon(picture), Nil) :: Nil
    )
  def btnViewList(key:VDomKey, action: ()=>Unit) =
    iconButton(key,"view list","IconActionViewList",action)
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
  def withMargin(key: VDomKey, value: Int, theChild: ChildPair[OfDiv]) =
    child[OfDiv](key, MarginWrapper(value), theChild :: Nil)
  def withMargin(key: VDomKey, value: Int, children: List[ChildPair[OfDiv]]) =
    child[OfDiv](key, MarginWrapper(value), children)
  def withSideMargin(key:VDomKey,value:Int,theChild:ChildPair[OfDiv])=
    child[OfDiv](key,MarginSideWrapper(value),theChild::Nil)
  def withSideMargin(key:VDomKey,value:Int,children:List[ChildPair[OfDiv]])=
    child[OfDiv](key,MarginSideWrapper(value),children)
  def withPadding(key: VDomKey, value: Int, theChild: ChildPair[OfDiv]) =
    child[OfDiv](key, PaddingWrapper(value), theChild :: Nil)
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

  def textInput(key: VDomKey, value: String, change: String⇒Unit,label: String,showLabel:Boolean) =
    child[OfDiv](key, InputField("TextField",value, label,showLabel,deferSend = true)
      (inputAttributes, identity, Some(newValue ⇒ change(newValue))), Nil)

  private def instantToString(value: Option[Instant]): String =
    value.map(_.toEpochMilli.toString).getOrElse("")

  private def stringToInstant(value: String): Option[Instant] =
    if(value.nonEmpty) Some(Instant.ofEpochMilli(java.lang.Long.valueOf(value))) else None

  def dateInput(key: VDomKey, value: Option[Instant], change: Option[Instant]⇒Unit,label: String,showLabel:Boolean) =
    child[OfDiv](key, InputField("DateInput",value,label,showLabel, deferSend = false)(inputAttributes,
      instantToString, Some(newValue ⇒ change(stringToInstant(newValue)))), Nil)
  def timeInput(key:VDomKey, value:Option[Instant],label:String,showLabel:Boolean,change:Option[Instant]=>Unit)=
    child[OfDiv](key,InputField("TimeInput",value,label,showLabel, deferSend=false)(inputAttributes,
      instantToString,Some(newValue=>change(stringToInstant(newValue)))),Nil)

  def labeledText(key:VDomKey,content:String,label:String)=
    child[OfDiv](key,LabeledTextComponent(content,label),Nil)
  def divHeightWrapper(key:VDomKey,height:Int,theChild:ChildPair[OfDiv])=
    child[OfDiv](key,DivHeightWrapper(height),theChild::Nil)
  def divEmpty(key:VDomKey)=
    child[OfDiv](key,DivEmpty(),Nil)



}