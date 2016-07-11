
package ee.cone.base.material_impl

import java.time.{Duration, Instant, LocalTime}

import ee.cone.base.connection_api._
import ee.cone.base.material._
import ee.cone.base.util.Never
import ee.cone.base.vdom.Types._
import ee.cone.base.vdom._

sealed trait ValidationKey
case object RequiredValidationKey extends ValidationKey
case object ErrorValidationKey extends ValidationKey
case object DefaultValidationKey extends ValidationKey

case class FieldPopupState[Value](objIdStr: String, attr: Attr[Value]) extends PopupState

class MaterialFields(
  fieldAttributes: FieldAttributes,
  handlerLists: CoHandlerLists,
  child: ChildPairFactory,
  utils: TagJsonUtils,
  tags: Tags,
  style: TagStyles,
  optionTags: OptionTagsI,
  buttonTags: ButtonTags,
  valueTypes: BasicValueTypes
) extends CoHandlerProvider {
  import fieldAttributes._
  import tags._
  import optionTags._
  import buttonTags._

  private def caption(attr: Attr[_]): String =
    handlerLists.single(AttrCaption(attr), ()⇒"???")

  private def converter[From,To](from: AttrValueType[From], to: AttrValueType[To]): From⇒To =
    handlerLists.single(ConverterKey(from,to), ()⇒Never())

  private def objCaption(obj: Obj) = converter(valueTypes.asObj, valueTypes.asString)(obj)
  private def objField(
    obj: Obj, attr: Attr[Obj], showLabel: Boolean, options: Seq[FieldOption]
  ): List[ChildPair[OfDiv]] = {
    val editable = getEditable(obj,options)
    val visibleLabel = if(showLabel) caption(attr) else ""
    val valueType = valueTypes.asObj
    val vObj = obj(attr)

    val value = if(vObj(aNonEmpty)) objCaption(vObj) else ""
    if(!editable){ return  List(labeledText("1",visibleLabel,value)) }

    //val key = popupKey(obj,attr)
    val (isOpened,popupToggle)=attrPopupAction[Obj](obj,attr)

    def items = handlerLists.single(AttrValueOptions(attr),()⇒Never())(obj)
    val input = inputField(
      "1", visibleLabel, value, v => if(v.isEmpty) obj(attr) = converter(valueTypes.asString,valueType)(v),
      deferSend = false, alignRight = false, getValidationKey(obj,attr)
    )
    val popup = (if(isOpened) items else Nil)
      .map(item⇒(item,objCaption(item))).sortBy(_._2)
      .map{ case(item,caption) ⇒ option(item(aObjIdStr),caption)(() ⇒ obj(attr) = item) }
    val btn =
      if(popup.nonEmpty) iconButton("less","less",IconNavigationExpandLess)(popupToggle)
      else iconButton("more","more",IconNavigationExpandMore)(popupToggle)
    btnInputPopup(btn, input, popup)
  }

  // obj, attr, showLabel; editable, defer, align; options
  private def booleanField(
    obj: Obj, attr: Attr[Boolean], showLabel: Boolean, options: Seq[FieldOption]
  ): List[ChildPair[OfDiv]] = {
    val editable = getEditable(obj,options)
    val visibleLabel = if(showLabel) caption(attr) else ""
    List(checkBox("1", visibleLabel, obj(attr), if(editable) obj(attr)=_ else _⇒()))
  }

  private def strField(
    obj: Obj, attr: Attr[String], showLabel: Boolean, options: Seq[FieldOption]
  ): List[ChildPair[OfDiv]] = {
    val editable = getEditable(obj,options)
    val visibleLabel = if(showLabel) caption(attr) else ""
    val value = obj(attr)
    val isPassword = options.contains(IsPasswordFieldOption)
    val theField =
      if(editable) List(inputField(
        "1", visibleLabel, value, obj(attr) = _,
        deferSend(options), alignRight=false, getValidationKey(obj,attr),
        isPassword
      ))
      else if(value.nonEmpty && !isPassword) List(labeledText("1", visibleLabel, value))
      else Nil
    if(!showLabel || theField.isEmpty) theField
    else if(isPassword)
      List(iconInput("1",IconSocialPerson)(theField))
    else if(options.contains(IsPersonFieldOption))
      List(iconInput("1",IconActionLock)(theField))
    else theField
  }

  private def durationField(
    obj: Obj, attr: Attr[Option[Duration]], showLabel: Boolean, options: Seq[FieldOption]
  ): List[ChildPair[OfDiv]] = {
    val editable = getEditable(obj,options)
    val visibleLabel = if(showLabel) caption(attr) else ""
    val valueType = valueTypes.asDuration
    val value = converter(valueType, valueTypes.asString)(obj(attr))
    if(!editable) { return List(labeledText("1",visibleLabel,value,alignRight = true))}

    //val key = popupKey(obj,attr)
    //println(popupOpened)
    val (isOpened,popupToggle)=attrPopupAction[Option[Duration]](obj,attr)
    val btn = iconButton("btnClock","clock",IconActionSchedule)(popupToggle)

    val input = inputField(
      "1", visibleLabel, value, v ⇒ obj(attr) = converter(valueTypes.asString,valueType)(v),
      deferSend=true, alignRight=true, getValidationKey(obj,attr)
    )

    val popup = if(!isOpened) Nil else List(
      div("popup",style.minWidth(280))(List(
        clockDialog("clock",value/*may be not 'value' later*/,Some{ newVal =>
          obj(attr) = converter(valueTypes.asString,valueType)(newVal)
          popupToggle()
        })
      ))
    )
    btnInputPopup(btn,input,popup)
    //input::Nil
  }

  private def controlGroup(children: List[ChildPair[OfDiv]]) =
    div("control",style.padding(10),style.alignRight)(children)

  private def dateField(
    obj: Obj, attr: Attr[Option[Instant]], showLabel: Boolean, options: Seq[FieldOption]
  ): List[ChildPair[OfDiv]] = {
    val editable = getEditable(obj,options)
    val visibleLabel = if(showLabel) caption(attr) else ""
    val valueType = valueTypes.asInstant
    val value = converter(valueType, valueTypes.asString)(obj(attr))
    if(!editable) { return List(labeledText("1", visibleLabel, value,alignRight = true))}

    //val key = popupKey(obj,attr)
    val (isOpened, popupToggle) = attrPopupAction[Option[Instant]](obj, attr)
    val btn = iconButton("btnCalendar", "calendar",IconActionDateRange)(popupToggle)
    val controlGrp = controlGroup(List(
      raisedButton("1", "Today")(() => {
        obj(attr) = Option(Instant.now())
        popupToggle()
      })
    ))



    val input = inputField(
      "1", visibleLabel, value, v ⇒ obj(attr) = converter(valueTypes.asString, valueType)(v),
      deferSend = true, alignRight = true, getValidationKey(obj, attr)
    )

    val popup = if (!isOpened) Nil
    else calendarDialog("calendar", value /*may be not 'value' later*/ , Some { newVal =>
      obj(attr) = converter(valueTypes.asString, valueType)(newVal)
      popupToggle()
    }) :: controlGrp :: Nil
    btnInputPopup(btn, input, popup)

  }

  private def timeField(
    obj: Obj, attr: Attr[Option[LocalTime]], showLabel: Boolean, options: Seq[FieldOption]
  ): List[ChildPair[OfDiv]] = {
    val editable = getEditable(obj,options)
    val visibleLabel = if(showLabel) caption(attr) else ""
    val valueType = valueTypes.asLocalTime
    val value = converter(valueType, valueTypes.asString)(obj(attr))

    if(!editable) { return List(labeledText("1", visibleLabel, value,alignRight = true))}
    val (isOpened, popupToggle) = attrPopupAction[Option[LocalTime]](obj,attr)
    val btn = iconButton("btnClock","clock",IconActionSchedule)(popupToggle)

    val input = inputField(
      "1", visibleLabel, value, v ⇒ obj(attr) = converter(valueTypes.asString,valueType)(v),
      deferSend=true, alignRight=true, getValidationKey(obj,attr)
    )
    val controlGrp = controlGroup(List(
      raisedButton("1", "Now")(() => {
        obj(attr) = Option(LocalTime.now())
        popupToggle()
      })
    ))
    val popup = if(!isOpened) Nil else List(
      div("popup",style.minWidth(280))(List(
        clockDialog("clock",value/*may be not 'value' later*/,Some{ newVal =>
          obj(attr) = converter(valueTypes.asString,valueType)(newVal)
          popupToggle()
        }),
        controlGrp
      ))
    )
    btnInputPopup(btn,input,popup)
  }

  private def decimalField(
    obj: Obj, attr: Attr[Option[BigDecimal]], showLabel: Boolean, options: Seq[FieldOption]
  ) = {
    val editable = getEditable(obj,options)
    val visibleLabel = if(showLabel) caption(attr) else ""
    val valueType = valueTypes.asBigDecimal
    val value = converter(valueType, valueTypes.asString)(obj(attr))
    if(editable) List(inputField(
      "1", visibleLabel, value, v ⇒ obj(attr) = converter(valueTypes.asString,valueType)(v),
      deferSend(options), alignRight=true, getValidationKey(obj,attr)
    ))
    else if(value.nonEmpty) List(labeledText("1", visibleLabel, value,alignRight=true))
    else Nil
  }

  private def getValidationKey[Value](obj: Obj, attr: Attr[Value]): ValidationKey = {
    val states = obj(aValidation).get(attr)
    if(states.isEmpty) DefaultValidationKey
    else if(states.exists(_.isError)) ErrorValidationKey
    else RequiredValidationKey
  }

  private def deferSend(options: Seq[FieldOption]) =
    options.collectFirst{ case o: DeferSendFieldOption ⇒ o.on }.getOrElse(true)

  private def getEditable(obj: Obj, options: Seq[FieldOption]) =
    options.collectFirst{ case o: EditableFieldOption ⇒ o.on }.getOrElse(obj(aIsEditing))

  private def attrPopupAction[Value](obj: Obj, attr: Attr[Value]): (Boolean,()=>Unit) = {
    val objNonEmpty = obj(aNonEmpty)
    def key = FieldPopupState(obj(aObjIdStr),attr)
    if(objNonEmpty) popupAction(key) else {
      val value = obj(attr) //is just empty
      (false, ()=>{
        obj(attr) = value
        popupAction(key) match { case (_,toggle) ⇒ toggle() }
      })
    }
  }

  private def btnInputPopup(btn: ChildPair[OfDiv], input: ChildPair[OfDiv], popup: List[ChildPair[OfDiv]]) = {
    val collapsed = List(btnInput("btnInput")(btn, input))
    List(popupBox("1",collapsed,popup))
  }

  def handlers = List(
    CoHandler(ViewField(valueTypes.asBoolean))(booleanField),
    CoHandler(ViewField(valueTypes.asString))(strField),
    CoHandler(ViewField(valueTypes.asDuration))(durationField),
    CoHandler(ViewField(valueTypes.asInstant))(dateField),
    CoHandler(ViewField(valueTypes.asLocalTime))(timeField),
    CoHandler(ViewField(valueTypes.asBigDecimal))(decimalField),
    CoHandler(ViewField(valueTypes.asObj))(objField)
  )

  ////////////////////

  def iconInput(key: VDomKey,picture:TagName)(input: List[ChildPair[OfDiv]])=
    div(key,style.noWrap)(List(
      tag("icon",picture, style.relative,style.displayInlineBlock,style.width(36))(Nil), //if(focused) Some("rgb(0,188,212)")
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
    )(utils, Some(change))

    div(key,style.relative)(List(
      child[OfDiv](key, input, Nil),
      child[OfDiv]("close",InputCloseIconButton("",label)(Some(()=>change(""))),
        if(value.isEmpty) Nil else
          List(tag("icon",IconNavigationClose,style.width(16),style.height(16))(Nil))
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
  def calendarDialog(key:VDomKey,date:String,action: Option[(String)=>Unit]) =
    child[OfDiv](key,CalendarDialog(date)(action),Nil)
  def clockDialog(key: VDomKey,time:String,action: Option[(String)=>Unit]) =
    child[OfDiv](key,ClockDialog(time)(action),Nil)
}

case object IconNavigationClose extends TagName("IconNavigationClose")
case object IconSocialPerson extends TagName("IconSocialPerson")
case object IconActionLock extends TagName("IconActionLock")
case object IconActionDateRange extends TagName("IconActionDateRange")
case object IconNavigationExpandMore extends TagName("IconNavigationExpandMore")
case object IconNavigationExpandLess extends TagName("IconNavigationExpandLess")
case object IconActionSchedule extends TagName("IconActionSchedule")

case class InputField(
  label: String, value: String,
  deferSend: Boolean, alignRight: Boolean, fieldValidationState: ValidationKey,
  isPassword: Boolean = false
)(
  input: TagJsonUtils, val onChange: Option[String=>Unit]
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
    input.appendInputAttributes(builder, value, deferSend)
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