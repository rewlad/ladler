package ee.cone.base.test_loots

import java.time.{Duration, Instant, LocalTime}

import ee.cone.base.connection_api._
import ee.cone.base.util.Never
import ee.cone.base.vdom.{ChildPair, OfDiv, Tags}

case class ViewField[Value](asType: AttrValueType[Value])
  extends EventKey[(Obj,Attr[Value],Boolean,Seq[FieldOption])=>List[ChildPair[OfDiv]]]
trait FieldOption
case class EditableFieldOption(on: Boolean) extends FieldOption
case class DeferSendFieldOption(on: Boolean) extends FieldOption
case object IsPasswordFieldOption extends FieldOption

case class AttrValueOptions(attr: Attr[Obj]) extends EventKey[Obj⇒List[Obj]]

class Popup(var opened: PopupState = ClosedPopupState)
trait PopupState
case object ClosedPopupState extends PopupState
case class FieldPopupState[Value](objIdStr: String, attr: Attr[Value]) extends PopupState

trait FieldAttributes {
  def aNonEmpty: Attr[Boolean]
  def aValidation: Attr[ObjValidation]
  def aIsEditing: Attr[Boolean]
  def aObjIdStr: Attr[String]
}

class MaterialFields(
  fieldAttributes: FieldAttributes,
  handlerLists: CoHandlerLists,
  tags: Tags,
  materialTags: MaterialTags,
  flexTags: FlexTags,
  popupState: Popup,
  asDuration: AttrValueType[Option[Duration]],
  asInstant: AttrValueType[Option[Instant]],
  asLocalTime: AttrValueType[Option[LocalTime]],
  asBigDecimal: AttrValueType[Option[BigDecimal]],
  asDBObj: AttrValueType[Obj],
  asString: AttrValueType[String],
  asBoolean: AttrValueType[Boolean]
) extends CoHandlerProvider {
  import fieldAttributes._
  import materialTags._
  import flexTags.divAlignWrapper

  private def caption(attr: Attr[_]): String =
    handlerLists.single(AttrCaption(attr), ()⇒"???")

  private def converter[From,To](from: AttrValueType[From], to: AttrValueType[To]): From⇒To =
    handlerLists.single(ConverterKey(from,to), ()⇒Never())

  private def popupKey[Value](obj: Obj, attr: Attr[Value]): PopupState =
    FieldPopupState(obj(aObjIdStr),attr)

  private def objCaption(obj: Obj) = converter(asDBObj, asString)(obj)
  private def objField(
    obj: Obj, attr: Attr[Obj], showLabel: Boolean, options: Seq[FieldOption]
  ): List[ChildPair[OfDiv]] = {
    val editable = getEditable(obj,options)
    val visibleLabel = if(showLabel) caption(attr) else ""
    val valueType = asDBObj
    val vObj = obj(attr)

    val value = if(vObj(aNonEmpty)) objCaption(vObj) else ""
    if(!editable){ return  List(labeledText("1",visibleLabel,value)) }

    //val key = popupKey(obj,attr)
    val (isOpened,popupToggle)=popupAction[Obj](obj,attr)

    def items = handlerLists.single(AttrValueOptions(attr),()⇒Never())(obj)
    val input = inputField(
      "1", visibleLabel, value, v => if(v.isEmpty) obj(attr) = converter(asString,valueType)(v),
      deferSend = false, alignRight = false, getValidationKey(obj,attr)
    )
    val popup = (if(isOpened) items else Nil)
      .map(item⇒(item,objCaption(item))).sortBy(_._2)
      .map{ case(item,caption) ⇒
        divClickable(item(aObjIdStr),Some{ ()⇒
          obj(attr) = item
          popupToggle()
        },divNoWrap("1",withDivMargin("1",5,divBgColorHover("1",MenuItemHoverColor,withPadding("1",10,tags.text("1",caption))))))
      }
    val btn = if(popup.nonEmpty) btnExpandLess("less",popupToggle)
    else btnExpandMore("more",popupToggle)
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
    if(editable) List(inputField(
      "1", visibleLabel, value, obj(attr) = _,
      deferSend(options), alignRight=false, getValidationKey(obj,attr),
      isPassword
    ))
    else if(value.nonEmpty && !isPassword) List(labeledText("1", visibleLabel, value))
    else Nil
  }

  private def durationField(
    obj: Obj, attr: Attr[Option[Duration]], showLabel: Boolean, options: Seq[FieldOption]
  ): List[ChildPair[OfDiv]] = {
    val editable = getEditable(obj,options)
    val visibleLabel = if(showLabel) caption(attr) else ""
    val valueType = asDuration
    val value = converter(valueType, asString)(obj(attr))
    if(!editable) { return List(labeledText("1",visibleLabel,value,alignRight = true))}

    //val key = popupKey(obj,attr)
    //println(popupOpened)
    val (isOpened,popupToggle)=popupAction[Option[Duration]](obj,attr)
    val btn = btnScheduleClock("btnClock",popupToggle)

    val input = inputField(
      "1", visibleLabel, value, v ⇒ obj(attr) = converter(asString,valueType)(v),
      deferSend=true, alignRight=true, getValidationKey(obj,attr)
    )

    val popup = if(!isOpened) Nil
    else  withMinWidth("minWidth",280,clockDialog("clock",value/*may be not 'value' later*/,Some{ newVal =>
      obj(attr) = converter(asString,valueType)(newVal)
      popupToggle()
    })::Nil)::Nil
    btnInputPopup(btn,input,popup)
    //input::Nil
  }

  private def dateField(
    obj: Obj, attr: Attr[Option[Instant]], showLabel: Boolean, options: Seq[FieldOption]
  ): List[ChildPair[OfDiv]] = {
    val editable = getEditable(obj,options)
    val visibleLabel = if(showLabel) caption(attr) else ""
    val valueType = asInstant
    val value = converter(valueType, asString)(obj(attr))
    if(!editable) { return List(labeledText("1", visibleLabel, value,alignRight = true))}

    //val key = popupKey(obj,attr)
    val (isOpened, popupToggle) = popupAction[Option[Instant]](obj, attr)
    val btn = btnDateRange("btnCalendar", popupToggle)
    val controlGrp = divSimpleWrapper("1", withPadding("1", 10, divAlignWrapper("1", "right", "", btnRaised("1", "Today")(() => {
      obj(attr) = Option(Instant.now())
      popupToggle()
    }) :: Nil)))

    val input = inputField(
      "1", visibleLabel, value, v ⇒ obj(attr) = converter(asString, valueType)(v),
      deferSend = true, alignRight = true, getValidationKey(obj, attr)
    )

    val popup = if (!isOpened) Nil
    else calendarDialog("calendar", value /*may be not 'value' later*/ , Some { newVal =>
      obj(attr) = converter(asString, valueType)(newVal)
      popupToggle()
    }) :: controlGrp :: Nil
    btnInputPopup(btn, input, popup)

  }

  private def timeField(
    obj: Obj, attr: Attr[Option[LocalTime]], showLabel: Boolean, options: Seq[FieldOption]
  ): List[ChildPair[OfDiv]] = {
    val editable = getEditable(obj,options)
    val visibleLabel = if(showLabel) caption(attr) else ""
    val valueType = asLocalTime
    val value = converter(valueType, asString)(obj(attr))

    if(!editable) { return List(labeledText("1", visibleLabel, value,alignRight = true))}
    val (isOpened, popupToggle) = popupAction[Option[LocalTime]](obj,attr)
    val btn = btnScheduleClock("btnClock",popupToggle)

    val input = inputField(
      "1", visibleLabel, value, v ⇒ obj(attr) = converter(asString,valueType)(v),
      deferSend=true, alignRight=true, getValidationKey(obj,attr)
    )
    val controlGrp = divSimpleWrapper("1", withPadding("1", 10, divAlignWrapper("1", "right", "", btnRaised("1", "Now")(() => {
      obj(attr) = Option(LocalTime.now())
      popupToggle()
    }) :: Nil)))
    val popup = if(!isOpened) Nil
    else withMinWidth("minWidth",280,clockDialog("clock",value/*may be not 'value' later*/,Some{ newVal =>
      obj(attr) = converter(asString,valueType)(newVal)
      popupToggle()
    })::controlGrp::Nil)::Nil
    btnInputPopup(btn,input,popup)
  }

  private def decimalField(
    obj: Obj, attr: Attr[Option[BigDecimal]], showLabel: Boolean, options: Seq[FieldOption]
  ) = {
    val editable = getEditable(obj,options)
    val visibleLabel = if(showLabel) caption(attr) else ""
    val valueType = asBigDecimal
    val value = converter(valueType, asString)(obj(attr))
    if(editable) List(inputField(
      "1", visibleLabel, value, v ⇒ obj(attr) = converter(asString,valueType)(v),
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

  private def popupAction[Value](obj: Obj, attr: Attr[Value]): (Boolean,()=>Unit) = {
    val objNonEmpty = obj(aNonEmpty)
    def key = popupKey(obj,attr)
    if(objNonEmpty){
      val isOpened = key == popupState.opened
      (isOpened, () => popupState.opened = if(isOpened) ClosedPopupState else key)
    } else {
      val value = obj(attr) //is just empty
      (false, ()=>{
        obj(attr) = value
        popupState.opened = key
      })
    }
  }

  private def btnInputPopup(btn: ChildPair[OfDiv], input: ChildPair[OfDiv], popup: List[ChildPair[OfDiv]]) = {
    val collapsed = List(btnInput("btnInput")(btn, input))
    List(fieldPopupBox("1",collapsed,popup))
  }

  def handlers = List(
    CoHandler(ViewField(asBoolean))(booleanField),
    CoHandler(ViewField(asString))(strField),
    CoHandler(ViewField(asDuration))(durationField),
    CoHandler(ViewField(asInstant))(dateField),
    CoHandler(ViewField(asLocalTime))(timeField),
    CoHandler(ViewField(asBigDecimal))(decimalField),
    CoHandler(ViewField(asDBObj))(objField)
  )
}
