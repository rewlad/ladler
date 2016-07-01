package ee.cone.base.test_loots

import ee.cone.base.vdom._
import ee.cone.base.vdom.Types._

sealed trait ValidationKey
case object RequiredValidationKey extends ValidationKey
case object ErrorValidationKey extends ValidationKey
case object DefaultValidationKey extends ValidationKey

case class SVGIcon(tp: String, attr: List[TagStyle]) extends VDomValue {
  def appendJson(builder: JsonBuilder) = {
    builder.startObject()
    builder.append("tp").append(tp)
    builder.append("style"); {
      builder.startObject()
      attr.foreach(_.appendStyle(builder))
      builder.end()
    }
    builder.end()
  }
}

case class InputField(
  label: String, value: String,
  deferSend: Boolean, alignRight: Boolean, fieldValidationState: ValidationKey,
  isPassword: Boolean = false
)(
  input: InputAttributes, val onChange: Option[String=>Unit]
) extends VDomValue with OnChangeReceiver {
  def appendJson(builder: JsonBuilder) = {
    builder.startObject()
    builder.append("tp").append("TextField")
    builder.append("name").append("text")
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
      builder.append("marginLeft").append("-15px")
      builder.end()
    }
    builder.end()
  }
}

case class InputCloseIconButton(tooltip:String,label:String)(val onClick:Option[()=>Unit]) extends VDomValue with OnClickReceiver{
  def appendJson(builder: JsonBuilder) = {
    builder.startObject()
    builder.append("tp").append("IconButtonEx")
    if(tooltip.nonEmpty) builder.append("tooltip").append(tooltip) //todo: color str
    onClick.foreach(_⇒ builder.append("onClick").append("send"))
    builder.append("style");{
      builder.startObject()
      builder.append("position").append("absolute")
      if(label.nonEmpty) builder.append("bottom").append("12px")
      else builder.append("bottom").append("14px")
      builder.append("width").append("")
      builder.append("height").append("")
      builder.append("padding").append("1px")
      builder.append("right").append("-5px")
      builder.end()
    }
    builder.append("iconStyle");{
      builder.startObject()
      builder.append("width").append("16px")
      builder.append("height").append("16px")
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
    builder.append("tp").append("IconButtonEx")
    if(tooltip.nonEmpty) builder.append("tooltip").append(tooltip) //todo: color str
    onClick.foreach(_⇒ builder.append("onClick").append("send"))
    builder.end()
  }
}



case class RaisedButton(label: String)(
  val onClick: Option[()⇒Unit]
) extends VDomValue with OnClickReceiver {
  def appendJson(builder: JsonBuilder) = {
    builder.startObject()
    builder.append("tp").append("RaisedButton")
    builder.append("primary").append(true)
    builder.append("label").append(label)
    onClick.foreach(_⇒ builder.append("onClick").append("send")) //try
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
    builder.append("fontSize").append("12px")
    builder.append("color").append("rgba(0,0,0,0.3)")
    builder.end()
    builder.end()
  }
}

case class LabeledTextElement(text:String,label:String,alignRight:Boolean) extends VDomValue{
  def appendJson(builder:JsonBuilder)={
    builder.startObject()
    builder.append("tp").append("LabeledText")
    builder.append("content").append(text)
    builder.append("label").append(label)
    builder.append("alignRight").append(alignRight)
    builder.append("style");{
      builder.startObject()
      builder.append("fontSize").append("13px")
      builder.end()
    }
    builder.end()
  }
}

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
case object AlertTextColor extends Color { def value="#f44336" }

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

class MaterialIconTags(
  child: ChildPairFactory, inputAttributes: InputAttributes, tags: Tags, popup: Popup,
  style: TagStyles, divTags: DivTags
) {
  import divTags._

  private def iconButton( tooltip: String, picture: String)(key: VDomKey, action: ()=>Unit) =
    child[OfDiv](key,
      IconButton(tooltip)(Some(action)),
      child("icon", SVGIcon(picture,Nil), Nil) :: Nil
    )
  def iconArrowUp()=
    child[OfDiv]("icon",SVGIcon("IconNavigationDropDown",List(style.alignMiddle)),Nil)
  def iconArrowDown()=
    child[OfDiv]("icon",SVGIcon("IconNavigationDropUp",List(style.alignMiddle)),Nil)
  //def btnViewList(key:VDomKey, action: ()=>Unit) =
  //  iconButton(key,"view list","IconActionViewList",action)
  //def btnFilterList(key: VDomKey, action: ()=>Unit) =
  //  iconButton(key,"filters","IconContentFilterList",action)
  //def btnClear(key: VDomKey, action: ()=>Unit) =
  //  iconButton(key,"clear sorting","IconContentClear",action)
  type Button = (VDomKey,()⇒Unit)⇒ChildPair[OfDiv]

  def btnSave: Button = iconButton("save","IconContentSave")
  def btnRestore: Button = iconButton("restore","IconActionRestore")
  def btnAdd: Button = iconButton("add","IconContentAdd")
  def btnRemove: Button = iconButton("remove","IconContentRemove")
  def btnModeEdit: Button = iconButton("edit","IconEditorModeEdit")
  def btnDelete: Button = iconButton("delete","IconActionDelete")
  def btnMenu: Button = iconButton("menu","IconNavigationMenu")
  def btnDateRange: Button = iconButton("calendar","IconActionDateRange")
  def btnExpandMore: Button = iconButton("more","IconNavigationExpandMore")
  def btnExpandLess: Button = iconButton("less","IconNavigationExpandLess")
  def btnScheduleClock: Button = iconButton("clock","IconActionSchedule")

  def iconInput(key: VDomKey,picture:String)(input: List[ChildPair[OfDiv]])=
    div(key,style.noWrap)(List(
      child[OfDiv]("icon",SVGIcon(picture, List(style.relative,style.displayInlineBlock,style.width(36))),Nil), //if(focused) Some("rgb(0,188,212)")
      div("input",style.displayInlineBlock,style.widthAllBut(36))(input)
    ))

  def inputField(
    key: VDomKey,
    label: String, value: String,
    change: String⇒Unit,
    deferSend: Boolean, alignRight: Boolean, fieldValidationState: ValidationKey,
    isPassword: Boolean = false
  ) = { //inp only
    val input = InputField(
      label, value,
      deferSend, alignRight, fieldValidationState,
      isPassword
    )(inputAttributes, Some(change))

    div(key,style.relative)(List(
      child[OfDiv](key, input, Nil),
      child[OfDiv]("close",InputCloseIconButton("",label)(Some(()=>change(""))),
        if(value.nonEmpty)
          List(child[OfDiv]("icon",SVGIcon("IconNavigationClose",List(style.width(16),style.height(16))),Nil))
        else Nil
      )
    ))
  }

  def btnInput(key: VDomKey)(btn:ChildPair[OfDiv],input:ChildPair[OfDiv])=
    div(key,style.noWrap)(List(
      div("input",style.displayInlineBlock, style.widthAllBut(48))(List(input)),
      div("icon",style.displayInlineBlock, style.width(48), style.alignMiddle)(List(btn))
    ))

  def labeledText(key:VDomKey, label:String, content:String, alignRight:Boolean = false) = //inp only
    if(label.nonEmpty) child[OfDiv](key,LabeledTextElement(content,label,alignRight),Nil)
    else tags.text(key, content)

  private def divBgColorHover(key:VDomKey,color:Color)(children: List[ChildPair[OfDiv]])=
    child[OfDiv](key,DivBgColorHover(color),children)
  def option(key:VDomKey, caption: String)(activate: ()⇒Unit) =
    divButton(key){ () ⇒
      activate()
      popup.opened = ClosedPopupState
    }(List(
      div("1",style.margin(5))(List(
        divBgColorHover("1",MenuItemHoverColor)(List(
          div("1",style.padding(10))(List(tags.text("1",caption)))
        ))
      ))
    ))

  def fieldPopupBox(key: VDomKey,fieldChildren:List[ChildPair[OfDiv]], popupChildren:List[ChildPair[OfDiv]]) = //cust
    div(key,style.displayBlock,style.widthAll,style.relative)(List(
      child[OfDiv](key+"box", FieldPopupBox(), fieldChildren),
      child[OfDiv](key+"popup", FieldPopupDrop(popupChildren.nonEmpty,maxHeight = Some(500)), popupChildren)
    ))


  def checkBox(key:VDomKey,label:String,checked:Boolean,check:Boolean=>Unit) = //inp,cust
    child[OfDiv](key,CheckBox(checked,label)(Some(v⇒check(v.nonEmpty))),Nil)
  def calendarDialog(key:VDomKey,date:String,action: Option[(String)=>Unit])={ //inp
    child[OfDiv](key,CalendarDialog(date)(action),Nil)
  }
  def clockDialog(key: VDomKey,time:String,action: Option[(String)=>Unit]) =
    child[OfDiv](key,ClockDialog(time)(action),Nil)
  def btnRaised(key: VDomKey, label: String)(action: ()=>Unit) = //inp,cust
    child[OfDiv](key, RaisedButton(label)(Some(action)), Nil)

}
