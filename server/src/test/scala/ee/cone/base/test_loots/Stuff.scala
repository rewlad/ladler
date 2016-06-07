package ee.cone.base.test_loots


import java.time.{Instant,LocalTime,Duration}

import ee.cone.base.connection_api._
import ee.cone.base.db._
import ee.cone.base.server.SenderOfConnection
import ee.cone.base.util.Never
import ee.cone.base.vdom.Types._
import ee.cone.base.vdom._

/*
object TimeZoneOffsetProvider{
  val defaultTimeZoneID="Europe/Tallinn" //EST
  def getZoneId=ZoneId.of(defaultTimeZoneID)
  def getZoneOffset={
    ZonedDateTime.now(ZoneId.of(defaultTimeZoneID)).getOffset
  }
}
*/
//  println(work(logAt.workStart))
/*
val workDuration=
 if(work(logAt.workStart).isEmpty||work(logAt.workStop).isEmpty)
   None
 else
    Some(Duration.between(
      work(logAt.workStart).getOrElse(Instant.now()).atZone(TimeZoneOffsetProvider.getZoneId),
      work(logAt.workStop).getOrElse(Instant.now()).atZone(TimeZoneOffsetProvider.getZoneId)
    ))
work.update(logAt.workDuration,workDuration)
val totalDuration=
      if(workList(entry).nonEmpty)
        Some(workList(entry).map{w:Obj=>w(logAt.workDuration).getOrElse(Duration.ZERO)}.reduce((a,b)=>a plus b))
      else None
*/

class FailOfConnection(
  sender: SenderOfConnection
) extends CoHandlerProvider {
  def handlers = CoHandler(FailEventKey){ e =>
    println(s"error: ${e.toString}")
    sender.sendToAlien("fail",e.toString) //todo
  } :: Nil
}

class BoatLogEntryAttributes(
  attr: AttrFactory,
  label: LabelFactory,

  asObj: AttrValueType[Obj],
  asString: AttrValueType[String],
  asInstant: AttrValueType[Option[Instant]],
  asLocalTime: AttrValueType[Option[LocalTime]],
  asDuration: AttrValueType[Option[Duration]],
  asBoolean: AttrValueType[Boolean]
)(
  val asEntry: Attr[Obj] = label("21f5378d-ee29-4603-bc12-eb5040287a0d"),
  val boat: Attr[Obj] = attr("b65d201b-8b83-41cb-85a1-c0cb2b3f8b18", asObj),
  val date: Attr[Option[Instant]] = attr("7680a4db-0a6a-45a3-bc92-c3c60db42ef9", asInstant),
  val durationTotal: Attr[Option[Duration]] = attr("6678a3d1-9472-4dc5-b79c-e43121d2b704", asDuration),
  val asConfirmed: Attr[Obj] = label("c54e4fd2-0989-4555-a8a5-be57589ff79d"),
  val confirmedBy: Attr[Obj] = attr("36c892a2-b5af-4baa-b1fc-cbdf4b926579", asObj),
  val confirmedOn: Attr[Option[Instant]] = attr("b10de024-1016-416c-8b6f-0620e4cad737", asInstant), //0x6709

  val dateFrom: Attr[Option[Instant]] = attr("6e260496-9534-4ca1-97f6-6b234ef93a55", asInstant),
  val dateTo: Attr[Option[Instant]] = attr("7bd7e2cb-d7fd-4b0f-b88b-b1e70dd609a1", asInstant),
  val hideConfirmed: Attr[Boolean] = attr("d49d29e0-d797-413e-afc6-2f62b06840ca", asBoolean),

  val asWork: Attr[Obj] = label("5cce1cf2-1793-4e54-8523-c810f7e5637a"),
  val workStart: Attr[Option[LocalTime]] = attr("41d0cbb8-56dd-44da-96a6-16dcc352ce99", asLocalTime),
  val workStop: Attr[Option[LocalTime]] = attr("5259ef2d-f4de-47b7-bc61-0cfe33cb58d3", asLocalTime),
  val workDuration: Attr[Option[Duration]] = attr("547917b2-7bb6-4240-9fba-06248109d3b6", asDuration),
  val workComment: Attr[String] = attr("5cec443e-8396-4d7b-99c5-422a67d4b2fc", asString),
  val entryOfWork: Attr[Obj] = attr("119b3788-e49a-451d-855a-420e2d49e476", asObj),

  val asBoat: Attr[Obj] = label("c6b74554-4d05-4bf7-8e8b-b06b6f64d5e2"),
  val boatName: Attr[String] = attr("7cb71f3e-e8c9-4f11-bfb7-f1d0ff624f09", asString)

)


trait Ref[T] {
  def value: T
  def value_=(value: T): Unit
}

class DataTablesState(currentVDom: CurrentVDom){
  private val widthOfTables = collection.mutable.Map[VDomKey,Float]()
  def widthOfTable(id: VDomKey) = new Ref[Float] {
    def value = widthOfTables.getOrElse(id,0.0f)
    def value_=(value: Float) = {
      widthOfTables(id) = value
      currentVDom.until(System.currentTimeMillis+200)
    }
  }
}

///////

class TestComponent(
  nodeAttrs: NodeAttrs,
  findAttrs: FindAttrs,
  filterAttrs: FilterAttrs,
  logAt: BoatLogEntryAttributes,
  userAttrs: UserAttrs,
  fuelingAttrs: FuelingAttrs,
  alienAttrs: AlienAccessAttrs,
  validationAttrs: ValidationAttributes,
  handlerLists: CoHandlerLists,
  attrFactory: AttrFactory,
  findNodes: FindNodes,
  mainTx: CurrentTx[MainEnvKey],
  alien: Alien,
  onUpdate: OnUpdate,
  tags: Tags,
  materialTags: MaterialTags,
  flexTags:FlexTags,
  currentVDom: CurrentVDom,
  dtTablesState: DataTablesState,
  searchIndex: SearchIndex,
  factIndex: FactIndex,
  filters: Filters,
  htmlTable: HtmlTable,
  users: Users,
  fuelingItems: FuelingItems,
  objIdFactory: ObjIdFactory,
  validationFactory: ValidationFactory,
  asDuration: AttrValueType[Option[Duration]],
  asInstant: AttrValueType[Option[Instant]],
  asLocalTime: AttrValueType[Option[LocalTime]],
  asDBObj: AttrValueType[Obj],
  asString: AttrValueType[String],
  uiStrings: UIStrings
)(
  val findEntry: SearchByLabelProp[String] = searchIndex.create(logAt.asEntry, findAttrs.justIndexed),
  val findWorkByEntry: SearchByLabelProp[Obj] = searchIndex.create(logAt.asWork, logAt.entryOfWork),
  val findBoat: SearchByLabelProp[String] = searchIndex.create(logAt.asBoat, findAttrs.justIndexed)
) extends CoHandlerProvider {
  import tags._
  import materialTags._
  import flexTags._
  import htmlTable._
  import findAttrs.nonEmpty
  import uiStrings.caption
  private def eventSource = handlerLists.single(SessionEventSource, ()⇒Never())

  private def getValidationKey[Value](obj: Obj, attr: Attr[Value]): ValidationKey = {
    val states = obj(validationAttrs.validation).get(attr)
    if(states.isEmpty) DefaultValidationKey
    else if(states.exists(_.isError)) ErrorValidationKey
    else RequiredValidationKey
  }


  private def booleanField(
      obj: Obj, attr: Attr[Boolean], showLabel: Boolean = false,
      editableOpt: Option[Boolean]=None
  ): List[ChildPair[OfDiv]] = {
    val editable = editableOpt.getOrElse(obj(alienAttrs.isEditing))
    val visibleLabel = if(showLabel) caption(attr) else ""
    List(checkBox("1", visibleLabel, obj(attr), if(editable) obj(attr)=_ else _⇒()))
  }

  private def strField(
    obj: Obj, attr: Attr[String], showLabel: Boolean,
    editableOpt: Option[Boolean]=None,
    deferSend: Boolean=true, alignRight: Boolean = false
  ): List[ChildPair[OfDiv]] = {
    val editable = editableOpt.getOrElse(obj(alienAttrs.isEditing))
    val visibleLabel = if(showLabel) caption(attr) else ""
    val value = obj(attr)
    if(editable) List(inputField(
      "1", TextFieldType, visibleLabel, value, obj(attr) = _,
      deferSend, alignRight, getValidationKey(obj,attr)
    ))
    else if(value.nonEmpty) List(labeledText("1", visibleLabel, value))
    else Nil
  }
  private def strPassField(
    obj: Obj, attr: Attr[String], showLabel: Boolean,
    editableOpt: Option[Boolean]=None,
    deferSend: Boolean=true
  ): List[ChildPair[OfDiv]] = {
    val editable = editableOpt.getOrElse(obj(alienAttrs.isEditing))
    val visibleLabel = if(showLabel) caption(attr) else ""
    val value = obj(attr)
    if(editable) List(inputField(
      "1", TextFieldType, visibleLabel, value, obj(attr) = _,
      deferSend, alignRight=false, getValidationKey(obj,attr)
    ))
    else Nil
  }

  private def durationField(
    obj: Obj, attr: Attr[Option[Duration]], showLabel: Boolean,
    editableOpt: Option[Boolean]=None
  ): List[ChildPair[OfDiv]] = {
    val editable = editableOpt.getOrElse(obj(alienAttrs.isEditing))
    val visibleLabel = if(showLabel) caption(attr) else ""
    val valueType = asDuration
    val value = uiStrings.converter(valueType, asString)(obj(attr))
    if(editable) List(inputField(
      "1", TimeFieldType, visibleLabel, value, v ⇒ obj(attr) = uiStrings.converter(asString,valueType)(v),
      deferSend=false, alignRight=true, getValidationKey(obj,attr)
    ))
    else List(labeledText("1",visibleLabel,value))

  }

  private def dateField(
    obj: Obj, attr: Attr[Option[Instant]], showLabel: Boolean,
    editableOpt: Option[Boolean]=None
  ): List[ChildPair[OfDiv]] = {
    val editable = editableOpt.getOrElse(obj(alienAttrs.isEditing))
    val visibleLabel = if(showLabel) caption(attr) else ""
    val valueType = asInstant
    val value = uiStrings.converter(valueType, asString)(obj(attr))
    if(!editable){ return List(labeledText("1", visibleLabel, value)) }

    val objIdStr = if(obj(nonEmpty)) obj(alien.objIdStr) else "empty"
    val key = s"$objIdStr-${objIdFactory.toUUIDString(attrFactory.attrId(attr))}"

    val popupCalendar =
      if(popupOpened != key) Nil
      else  withMinWidth("minWidth",320,calendarDialog("calendar",value,Some{ newVal =>
        obj(attr) = uiStrings.converter(asString,valueType)(newVal)
        popupToggle(key)
      })::Nil)::Nil

    val input = inputField(
      "1", TextFieldType, visibleLabel, value, v ⇒ obj(attr) = uiStrings.converter(asString,valueType)(v),
      deferSend = true, alignRight = true, getValidationKey(obj,attr)
    )

    List(fieldPopupBox(
      "calendar",
      List(btnInput("icon")(btnDateRange("btnCalendar",popupToggle(key)), input)),
      popupCalendar
    ))
  }

  private def timeField(
    obj: Obj, attr: Attr[Option[LocalTime]], showLabel: Boolean,
    editableOpt: Option[Boolean]=None
  ): List[ChildPair[OfDiv]] = {
    val editable = editableOpt.getOrElse(obj(alienAttrs.isEditing))
    val visibleLabel = if(showLabel) caption(attr) else ""

    val valueType = asLocalTime
    val value = uiStrings.converter(valueType, asString)(obj(attr))
    if(editable) List(inputField(
      "1", TimeFieldType, visibleLabel, value, v ⇒ obj(attr) = uiStrings.converter(asString,valueType)(v),
      deferSend=false, alignRight=true, getValidationKey(obj,attr)
    ))
    else List(labeledText("1", visibleLabel, value))
  }

  private var popupOpened = ""
  private def popupToggle(key: String)() =
    popupOpened = if(popupOpened == key) "" else key
  private def objCaption(obj: Obj) = uiStrings.converter(asDBObj, asString)(obj)

  private def objField(
    obj: Obj, attr: Attr[Obj], showLabel: Boolean,
    editableOpt: Option[Boolean]=None
  )(
    items: ()⇒List[Obj]=()⇒Nil
  ): List[ChildPair[OfDiv]] = {
    val editable = editableOpt.getOrElse(obj(alienAttrs.isEditing))
    val visibleLabel = if(showLabel) caption(attr) else ""
    val vObj = obj(attr)
    val notSelected = "(not selected)"

    val value = if(vObj(nonEmpty)) objCaption(vObj) else ""
    if(!editable){ return  List(labeledText("1",visibleLabel,value)) }
    def option(item: Obj, key: VDomKey, caption: String) = divClickable(key,Some{ ()⇒
      obj(attr) = item
      popupOpened = ""
    },divNoWrap("1",withDivMargin("1",5,divBgColorHover("1",MenuItemHoverColor,withPadding("1",10,text("1",caption))))))
    val objIdStr = if(obj(nonEmpty)) obj(alien.objIdStr) else "empty"
    val key = s"$objIdStr-${objIdFactory.toUUIDString(attrFactory.attrId(attr))}"
    val input = inputField(
      "1", TextFieldType, visibleLabel, value, _=>{},
      deferSend=false, alignRight = false, getValidationKey(obj,attr)
    )
    val rows = if(popupOpened != key) Nil
      else option(findNodes.noNode, "not_selected", notSelected) ::
        items().map(item ⇒ option(item, item(alien.objIdStr), objCaption(item)))
    val collapsed = btnInput("btnInput")(
      if(rows.nonEmpty) btnExpandLess("less",popupToggle(key)) else btnExpandMore("more",popupToggle(key)),
      input
    )::Nil
    List(fieldPopupBox("1",collapsed,rows))
  }

  ////

  private def emptyView(pf: String) =
    tags.root(List(tags.text("text", "Loading...")))

  private def wrapDBView(view: ()=>List[ChildPair[OfDiv]]): VDomValue =
    eventSource.incrementalApplyAndView { () ⇒
      root(withMinWidth("minWidth320",320,if(users.needToLogIn) loginView() else {
        val startTime = System.currentTimeMillis
        val res = view()
        val endTime = System.currentTimeMillis
        currentVDom.until(endTime + (endTime - startTime) * 10)
        res
      })::Nil)
    }

  private def paperWithMargin(key: VDomKey, child: ChildPair[OfDiv]*) =
    withMargin(key, 10, paper("paper")( withPadding(key, 10, child:_*)))

  def toggledSelectedRow(item: Obj) = List(
    Toggled(item(filterAttrs.isExpanded))(Some(()=> if(!item(filterAttrs.isExpanded)){
      filters.editing = findNodes.noNode
      item(filterAttrs.isExpanded) = true
    })),
    IsSelected(item(filterAttrs.isSelected))
  )
  def toggledRow(filterObj: Obj, id: ObjId) =
    Toggled(filterObj(filterAttrs.expandedItem)==id)(Some(()=>filterObj(filterAttrs.expandedItem)=id))

  private def mCell(key:VDomKey,minWidth: Int)(handle:(Boolean)=>List[ChildPair[OfDiv]])=
    cell(key,MinWidth(minWidth),VerticalAlignMiddle)(showLabel=>withSideMargin("1",10,handle(showLabel))::Nil)
  private def mmCell(key:VDomKey,minWidth: Int,maxWidth:Int)(handle:(Boolean)=>List[ChildPair[OfDiv]])=
    cell(key,MinWidth(minWidth),MaxWidth(maxWidth),VerticalAlignMiddle)(showLabel=>withSideMargin("1",10,handle(showLabel))::Nil)
  private def mCell(key:VDomKey,minWidth: Int,priority: Int)(handle:(Boolean)=>List[ChildPair[OfDiv]])=
    cell(key,MinWidth(minWidth),Priority(priority),VerticalAlignMiddle)(showLabel=>withSideMargin("1",10,handle(showLabel))::Nil)
  private def mcCell(key:VDomKey,minWidth: Int)(handle:(Boolean)=>List[ChildPair[OfDiv]])=
    cell(key,MinWidth(minWidth),TextAlignCenter,VerticalAlignMiddle)(showLabel=>withSideMargin("1",10,handle(showLabel))::Nil)
  private def mcCell(key:VDomKey,minWidth: Int, priority:Int)(handle:(Boolean)=>List[ChildPair[OfDiv]])=
    cell(key,MinWidth(minWidth),Priority(priority),TextAlignCenter,VerticalAlignMiddle)(showLabel=>withSideMargin("1",10,handle(showLabel))::Nil)

  def paperTable(key: VDomKey)(controls:List[ChildPair[OfDiv]],tableElements: List[TableElement with ChildOfTable]): ChildPair[OfDiv] = {
    val tableWidth = dtTablesState.widthOfTable(key)
    paperWithMargin(key,
      flexGrid("flexGrid",
        flexGridItemWidthSync("widthSync",w⇒tableWidth.value=w.toFloat,
          controls:::
          table("1",Width(tableWidth.value))(tableElements:_*)
        )
      )
    )
  }

  def selectAllCheckBox(itemList: ItemList) = List(
    checkBox("1","",
      itemList.filter(filterAttrs.selectedItems).nonEmpty,
      on ⇒
        if(on) itemList.selectAllListed()
        else itemList.filter(filterAttrs.selectedItems)=Set[ObjId]()
    )
  )

  def sortingHeader(itemList: ItemList, attr: Attr[_]):List[ChildPair[OfDiv]] = {
    val (action,reversed) = itemList.orderByAction(attr)
    val txt = text("1",caption(attr))::Nil
    val icon = reversed match {
      case None ⇒ List()
      case Some(false) ⇒ iconArrowDown()::Nil
      case Some(true) ⇒ iconArrowUp()::Nil
    }
    if(action.isEmpty) List(txt:_*)
    else List(divClickable("1",action,icon:::txt:_*))
  }

  private def addRemoveControlViewBase(itemList: ItemList)(add: ()⇒Unit) =
    if(itemList.isEditable) List(btnDelete("btnDelete", itemList.removeSelected),btnAdd("btnAdd", add))
    else Nil
  private def addRemoveControlView(itemList: ItemList) =
    addRemoveControlViewBase(itemList: ItemList){ ()⇒
      val item = itemList.add()
      item(alienAttrs.isEditing) = true
      item(filterAttrs.isExpanded) = true
    }


  private def iconCellGroup(key: VDomKey)(content: Boolean⇒List[ChildPair[OfDiv]]) = List(
    group(s"${key}_grp", MinWidth(50), MaxWidth(50), Priority(1), TextAlignCenter),
    mCell(key, 50)(content)
  )

  private def selectAllGroup(itemList: ItemList) =
    iconCellGroup("selected")(_⇒selectAllCheckBox(itemList))
  private def selectRowGroup(item: Obj) =
    iconCellGroup("selected")(_⇒booleanField(item, filterAttrs.isSelected, editableOpt = Some(true)))
  private def editAllGroup() = iconCellGroup("edit")(_⇒Nil)
  private def editRowGroupBase(on: Boolean)(action: ()⇒Unit) =
    iconCellGroup("edit")(_⇒if(on) List(btnModeEdit("btnCreate",action)) else Nil)
  private def editRowGroup(itemList: ItemList, item: Obj) =
    editRowGroupBase(itemList.isEditable){ () ⇒
      item(alienAttrs.isEditing) = !item(alienAttrs.isEditing)
      item(filterAttrs.isExpanded) = true
    }

  private def entryListView(pf: String) = wrapDBView{ ()=>{
    val editable = true //todo roles
    val filterObj = filters.filterObj(List(attrFactory.attrId(logAt.asEntry)))
    val filterList: List[Obj⇒Boolean] = {
      val value = filterObj(logAt.boat)(nodeAttrs.objId)
      if(value.nonEmpty) List((obj:Obj) ⇒ obj(logAt.boat)(nodeAttrs.objId)==value) else Nil
    } ::: {
      val value = filterObj(logAt.dateFrom)
      if(value.nonEmpty) List((obj:Obj) ⇒ obj(logAt.date).forall((v:Instant) ⇒ v.isAfter(value.get))) else Nil
    } ::: {
      val value = filterObj(logAt.dateTo)
      if(value.nonEmpty) List((obj:Obj) ⇒ obj(logAt.date).forall((v:Instant) ⇒ v.isBefore(value.get))) else Nil
    } ::: {
      val value = filterObj(logAt.hideConfirmed)
      if(value) List((obj:Obj) ⇒ !obj(logAt.asConfirmed)(nonEmpty) ) else Nil
    }
//logAt.dateFrom, logAt.dateTo, logAt.hideConfirmed,

    val itemList = filters.itemList(findEntry,findNodes.justIndexed,filterObj,filterList,editable)
    def go(entry: Obj) = currentVDom.relocate(s"/entryEdit/${entry(alien.objIdStr)}")

    List( //class LootsBoatLogList
      toolbar("Entry List"),

      withMaxWidth("1",1200,List(paperTable("dtTableList2")(
        controlPanel(inset = true)(
          List(flexGrid("controlGrid1",List(
            flexGridItem("1a",150,Some(200), boatSelectView(filterObj)),
            flexGridItem("2a",150,Some(200), dateField(filterObj, logAt.dateFrom, showLabel = true)),
            flexGridItem("3a",150,Some(200), dateField(filterObj, logAt.dateTo, showLabel = true)),
            flexGridItem("4a",150,Some(200), divHeightWrapper("1",72,
              divAlignWrapper("1","","bottom",withMargin("1",10,booleanField(filterObj, logAt.hideConfirmed, showLabel = true))::Nil)
            )::Nil)
          ))),
          addRemoveControlViewBase(itemList)(() ⇒ go(itemList.add()))
        ),
        List(
          row("row",List(MaxVisibleLines(2),IsHeader))(
            selectAllGroup(itemList) :::
            List(
              group("2_grp",MinWidth(150),Priority(3),TextAlignCenter),
              mCell("2",100)(_=>sortingHeader(itemList,logAt.boat)),
              mCell("3",150)(_=>sortingHeader(itemList,logAt.date)),
              mCell("4",180)(_=>sortingHeader(itemList,logAt.durationTotal)),
              mCell("5",100)(_=>sortingHeader(itemList,logAt.asConfirmed)),
              mCell("6",150)(_=>sortingHeader(itemList,logAt.confirmedBy)),
              mCell("7",150)(_=>sortingHeader(itemList,logAt.confirmedOn))
            ) :::
            editAllGroup()
          )
        ) :::
        itemList.list.map{ (entry:Obj)=>
          val entrySrcId = entry(alien.objIdStr)
          row(entrySrcId, MaxVisibleLines(2) :: toggledSelectedRow(entry))(
            selectRowGroup(entry) :::
            List(
              group("2_grp", MinWidth(150),Priority(3), TextAlignCenter),
              mCell("2",100)(showLabel=>objField(entry, logAt.boat, showLabel)()),
              mCell("3",150)(showLabel=>dateField(entry, logAt.date, showLabel)),
              mCell("4",180)(showLabel=>durationField(entry, logAt.durationTotal, showLabel)),
              mCell("5",100)(_=>
               {
                  val confirmed = entry(logAt.asConfirmed)
                  if(confirmed(nonEmpty))
                    List(materialChip("1","CONFIRMED")(None))
                  else Nil
               }
              ),
              mCell("6",150)(showLabel=>objField(entry, logAt.confirmedBy, showLabel)()),
              mCell("7",150)(showLabel=>dateField(entry, logAt.confirmedOn, showLabel))
            ) :::
            editRowGroupBase(on=true)(()⇒go(entry))
          )
        }
      )))
    )
  }}
  // currentVDom.invalidate() ?

  private def boatSelectView(obj: Obj) =
    objField(obj,logAt.boat,showLabel = true)(()⇒
      filters.itemList(findBoat, findNodes.justIndexed, findNodes.noNode, Nil, editable=false).list
    )

  private def entryEditView(pf: String) = wrapDBView { () =>
    val entryObj = findNodes.whereObjId(objIdFactory.toObjId(pf.tail))(logAt.asEntry)
    val isConfirmed = entryObj(logAt.asConfirmed)(nonEmpty)
    val validationStates = if(isConfirmed) Nil else
      fuelingItems.validation(entryObj) :::
      validationFactory.need[Obj](entryObj,logAt.boat,v⇒if(!v(nonEmpty)) Some("") else None) :::
      validationFactory.need[Option[Instant]](entryObj,logAt.date,v⇒if(v.isEmpty) Some("") else None)
    val validationContext = validationFactory.context(validationStates)
    val entry = if(isConfirmed) entryObj else validationContext.wrap(alien.wrapForEdit(entryObj)) /*todo roles*/
    val entryIdStr = entry(alien.objIdStr)

    List(
      toolbar("Entry Edit"),
      withMaxWidth("1",1200,List(
      paperWithMargin(s"$entryIdStr-1",
        flexGrid("flexGridEdit1",List(
          flexGridItem("1",500,None,List(
            flexGrid("FlexGridEdit11",List(
              flexGridItem("boat1",100,None,boatSelectView(entry)),
              flexGridItem("date",150,None,dateField(entry, logAt.date, showLabel = true)),
              flexGridItem("dur",170,None,List(divAlignWrapper("1","left","middle",
                durationField(entry,logAt.durationTotal, editableOpt = Some(false), showLabel = true)
              )))
            ))
          )),
          flexGridItem("2",500,None,List(
            flexGrid("flexGridEdit12",
              (if(!isConfirmed) Nil else List(
                flexGridItem("conf_by",150,None,objField(entry,logAt.confirmedBy, showLabel = true)()),
                flexGridItem("conf_on",150,None,dateField(entry, logAt.confirmedOn, showLabel = true))
              )) :::
              List(
                flexGridItem("conf_do",150,None,List(
                  divHeightWrapper("1",72,
                    divAlignWrapper("1","right","bottom",
                      if(isConfirmed) {
                        val entry = alien.wrapForEdit(entryObj)
                        List(
                          btnRaised("reopen", "Reopen") { () ⇒
                            entry(logAt.confirmedOn) = None
                            entry(logAt.confirmedBy) = findNodes.noNode
                          }
                        )
                      }
                      else if(validationStates.nonEmpty){
                        val state = validationStates.head
                        List(text("1",state.text,AlertTextColor.color))
                      }
                      else {
                        val user = eventSource.mainSession(userAttrs.authenticatedUser)
                        if(!user(nonEmpty)) List(text("1",s"User required",AlertTextColor.color))
                        else List(btnRaised("confirm","Confirm"){()⇒
                          entry(logAt.confirmedOn) = Option(Instant.now())
                          entry(logAt.confirmedBy) = user
                        })
                      }
                    ))
                ))
              )
            )
          )))
        )
      ))),

      withMaxWidth("2",1200,List(entryEditFuelScheduleView(entry, validationStates))),
      withMaxWidth("3",1200,List(entryEditWorkListView(entry)))
    )
  }


  def entryEditFuelScheduleView(entry: Obj, validationStates: List[ValidationState]): ChildPair[OfDiv] = {
    val entryIdStr = entry(alien.objIdStr)
    val filterObj = filters.filterObj(List(entry(nodeAttrs.objId)))
    val deferSend = validationStates.size > 2
    val validationContext = validationFactory.context(validationStates)

    def fuelingRowView(time: ObjId, isRF: Boolean) = {
      val fueling = validationContext.wrap(
        if(isRF) entry else fuelingItems.fueling(entry, time, wrapForEdit=entry(alienAttrs.isEditing))
      )
      val timeStr = if(isRF) "RF" else findNodes.whereObjId(time)(fuelingAttrs.time)
      row(timeStr,toggledRow(filterObj,time))(
        mCell("1",100,3)(showLabel=>
          if(isRF) List(text("1","Passed"))
          else List(labeledText("1",if(showLabel) "Time" else "",timeStr))
        ),
        mCell("2",150,1)(showLabel=>
          if(isRF) List(text("1","Received Fuel"))
          else durationField(fueling, fuelingAttrs.meHours, showLabel)
          /*text("c",if(fueling(fuelingAttrs.meHours).nonEmpty) "+" else "-" ) ::
            strField(fueling, fuelingAttrs.meHoursStr, showLabel, deferSend = false)*/
        ),
        mCell("3",100,1)(showLabel=>
          strField(fueling, fuelingAttrs.fuel, showLabel, deferSend=deferSend, alignRight = true)
        ),
        mCell("4",250,3)(showLabel=>
          strField(fueling, fuelingAttrs.comment, showLabel, deferSend=deferSend)
        ),
        mCell("5",150,2)(showLabel=>
          strField(fueling, fuelingAttrs.engineer, showLabel, deferSend=deferSend)
        ),
        mCell("6",150,2)(showLabel=>
          if(isRF) Nil
          else strField(fueling, fuelingAttrs.master, showLabel, deferSend=deferSend)
        )
      )
    }
    paperTable("dtTableEdit1")(Nil,List(
      row("row",IsHeader)(
        mCell("1",100,3)(_=>List(text("1","Time"))),
        mCell("2",150,1)(_=>List(text("1",caption(fuelingAttrs.meHours)))),
        mCell("3",100,1)(_=>List(text("1",caption(fuelingAttrs.fuel)))),
        mCell("4",250,3)(_=>List(text("1",caption(fuelingAttrs.comment)))),
        mCell("5",150,2)(_=>List(text("1",caption(fuelingAttrs.engineer)))),
        mCell("6",150,2)(_=>List(text("1",caption(fuelingAttrs.master))))
      ),
      fuelingRowView(fuelingAttrs.time00,isRF = false),
      fuelingRowView(fuelingAttrs.time08,isRF = false),
      fuelingRowView(objIdFactory.noObjId,isRF = true),
      fuelingRowView(fuelingAttrs.time24,isRF = false)
    ))
  }

  def entryEditWorkListView(entry: Obj): ChildPair[OfDiv] = {
    val entryIdStr = entry(alien.objIdStr)
    val filterObj = filters.filterObj(List(entry(nodeAttrs.objId),attrFactory.attrId(logAt.asWork)))
    val workList = filters.itemList(findWorkByEntry,entry,filterObj,Nil,entry(alienAttrs.isEditing))
    paperTable("dtTableEdit2")(
      controlPanel(inset = false)(Nil,addRemoveControlView(workList)),
      List(
        row("row",List(IsHeader))(
          selectAllGroup(workList) :::
          List(
            group("2_group",MinWidth(150)),
            mCell("2",100)(_=>sortingHeader(workList,logAt.workStart)),
            mCell("3",100)(_=>sortingHeader(workList,logAt.workStop)),
            mCell("4",150)(_=>sortingHeader(workList,logAt.workDuration)),
            mCell("5",250,3)(_=>sortingHeader(workList,logAt.workComment))
          ) :::
          editAllGroup()
        )
      ) :::
      workList.list.map { (work: Obj) =>
        val workSrcId = work(alien.objIdStr)
        row(workSrcId, toggledSelectedRow(work))(
          selectRowGroup(work) :::
          List(
            group("2_group",MinWidth(150)),
            mCell("2",100)(showLabel=>
              timeField(work, logAt.workStart, showLabel)
            ),
            mCell("3",100)(showLabel=>
              timeField(work, logAt.workStop, showLabel)
            ),
            mCell("4",150)(showLabel=>
              durationField(work, logAt.workDuration, showLabel, editableOpt = Some(false))
            ),
            mCell("5",250,3)(showLabel=>
              strField(work, logAt.workComment, showLabel)
            )
          ) :::
          editRowGroup(workList, work)
        )
      }
    )
  }

  private def boatListView(pf: String) = wrapDBView { () =>
    val filterObj = filters.filterObj(List(attrFactory.attrId(logAt.asBoat)))
    val itemList = filters.itemList(findBoat, findNodes.justIndexed, filterObj, Nil, editable=true) //todo roles
    List(
      toolbar("Boats"),
      withMaxWidth("maxWidth",600,
      paperTable("table")(
        controlPanel(inset = false)(Nil, addRemoveControlView(itemList)),
        List(
          row("head",List(IsHeader))(
            selectAllGroup(itemList) :::
            List(
              group("2_group",MinWidth(50)),
              mCell("1",250)(_⇒sortingHeader(itemList,logAt.boatName))
            ) :::
            editAllGroup()
          )
        ) :::
        itemList.list.map{boat ⇒
          val srcId = boat(alien.objIdStr)
          row(srcId,toggledSelectedRow(boat))(
            selectRowGroup(boat) :::
            List(
              group("2_group",MinWidth(50)),
              mCell("1",250)(showLabel⇒strField(boat, logAt.boatName, showLabel))
            ) :::
            editRowGroup(itemList, boat)
          )
        }
      )::Nil)
    )
  }
  private def controlPanel(inset:Boolean)(chld1:List[ChildPair[OfDiv]],chld2:List[ChildPair[OfDiv]])={
    val _content=withPadding("1",5,withSidePadding("1",8,
      divWrapper("1",Some("inline-block"),Some("1px"),Some("1px"),None,None,None,withMinHeight("1",48,List():_*)::Nil)::
        divWrapper("2",Some("inline-block"),Some("60%"),Some("60%"),None,None,None,chld1)::
        divWrapper("3",None,None,None,None,Some("right"),None,chld2)::Nil
    ))
    if(inset) divSimpleWrapper("tableControl",paper("1",inset)(_content))::Nil
    else divSimpleWrapper("tableControl",_content)::Nil
  }
  //// users

  private def usernameField(obj: Obj, showLabel: Boolean) = {
    val field = strField(obj, userAttrs.username, showLabel)
    if(!showLabel) field else if(field.nonEmpty) List(iconInput("1","IconSocialPerson")(field)) else Nil
  }
  private def passwordField(obj: Obj, attr: Attr[String], showLabel: Boolean) = {
    val field = strPassField(obj, attr, showLabel, deferSend = false)
    if(!showLabel) field else if(field.nonEmpty) List(iconInput("1","IconActionLock")(field)) else Nil
  }

  private def loginView() = {
    val showLabel = true
    val dialog = filters.filterObj(List(attrFactory.attrId(userAttrs.asActiveUser)))
    List(withMaxWidth("1",400,paperWithMargin("login",
      divSimpleWrapper("1",usernameField(dialog, showLabel):_*),
      divSimpleWrapper("2",passwordField(dialog, userAttrs.unEncryptedPassword, showLabel):_*),
      divSimpleWrapper("3", divAlignWrapper("1","right","top",users.loginAction(dialog).map(btnRaised("login","LOGIN")(_)).toList))
    )::Nil))

  }



  private def userListView(pf: String) = wrapDBView { () =>
    val filterObj = filters.filterObj(List(attrFactory.attrId(userAttrs.asUser)))
    val userList = filters.itemList(users.findAll, findNodes.justIndexed, filterObj, Nil, editable = true) //todo roles
    val showPasswordCols = filters.editing(userAttrs.asUser)(nonEmpty)
    List(
      toolbar("Users"),
      paperTable("table")(
        controlPanel(inset = false)(Nil, addRemoveControlView(userList)),
        row("head",List(IsHeader))(
          selectAllGroup(userList) :::
          List(
            group("2_grp", MinWidth(300)),
            mCell("1",250)(_⇒sortingHeader(userList,userAttrs.fullName)),
            mCell("2",250)(_⇒sortingHeader(userList,userAttrs.username)),
            mmCell("3",100,150)(_⇒sortingHeader(userList,userAttrs.asActiveUser))
          ) :::
          (if(showPasswordCols) List(
            group("3_grp",MinWidth(150)),
            mCell("4",150)(_⇒sortingHeader(userList,userAttrs.unEncryptedPassword)),
            mCell("5",150)(_⇒sortingHeader(userList,userAttrs.unEncryptedPasswordAgain)),
            mCell("6",150)(_⇒Nil)
          ) else Nil) :::
          editAllGroup()
        ) ::
        userList.list.map{ user ⇒
          val srcId = user(alien.objIdStr)
          row(srcId,toggledSelectedRow(user))(
            selectRowGroup(user) :::
            List(
              group("2_grp", MinWidth(300)),
              mCell("1",250)(showLabel⇒strField(user, userAttrs.fullName, showLabel)),
              mCell("2",250)(showLabel⇒usernameField(user, showLabel)),
              mmCell("3",100,150)(showLabel⇒
                if(user(userAttrs.asActiveUser)(findAttrs.nonEmpty)) List(materialChip("0","Active")(None)) else Nil
              )
            ) :::
            (if(showPasswordCols) List(
              group("3_grp",MinWidth(150)),
              mCell("4",150)(showLabel⇒passwordField(user, userAttrs.unEncryptedPassword, showLabel)),
              mCell("5",150)(showLabel⇒passwordField(user, userAttrs.unEncryptedPasswordAgain, showLabel)),
              mCell("6",150) { _ =>
                users.changePasswordAction(user).map(
                  btnRaised("doChange", "Change Password")(_)
                ).toList
              }
            ) else Nil) :::
            editRowGroup(userList, user)
          )
        }
      )
    )
  }

  //// events
  private def eventListView(pf: String) = wrapDBView { () =>
    List(
      toolbar("Events"),
      paperTable("table")(Nil,
        row("head", IsHeader)(
          mCell("1",250)(_⇒List(text("text", "Event"))),
          mCell("2",250)(_⇒Nil)
        ) ::
        eventSource.unmergedEvents.map(alien.wrapForEdit).map { ev =>
          val srcId = ev(alien.objIdStr)
          row(srcId)(
            mCell("1",250)(_⇒List(text("text", ev(alienAttrs.comment)))),
            mCell("2",250)(_⇒List(btnRemove("btn", () => eventSource.addUndo(ev))))
          )
        }
      )
    )
  }

  private def eventToolbarButtons() = if (eventSource.unmergedEvents.isEmpty) Nil
    else List(
      btnRestore("events", () ⇒ currentVDom.relocate("/eventList")),
      btnSave("save", ()⇒eventSource.addRequest())
    )
  private def eventListHandlers = CoHandler(ViewPath("/eventList"))(eventListView) :: Nil

  //// calculations

  private def calcWorkDuration(on: Boolean, work: Obj): Unit = {
    work(logAt.workDuration) = if(!on) None else {
      val start = work(logAt.workStart).get
      val stop = work(logAt.workStop).get
      if(start.isBefore(stop)) Option(Duration.between(start, stop))
      else if(LocalTime.of(0,0)==start && start==stop) Option(Duration.ofDays(1))
      else None
    }
  }
  private def calcEntryDuration(on: Boolean, work: Obj): Unit = {
    val entry = work(logAt.entryOfWork)
    val was = entry(logAt.durationTotal).getOrElse(Duration.ofSeconds(0L))
    val delta = work(logAt.workDuration).get
    entry(logAt.durationTotal) =
      Option(if(on) was.plus(delta) else was.minus(delta))
  }
  private def calcConfirmed(on: Boolean, entry: Obj): Unit = {
    entry(logAt.asConfirmed) = if(on) entry else findNodes.noNode
  }
  private def calcHandlers() =
    onUpdate.handlers(List(logAt.asWork,logAt.workStart,logAt.workStop).map(attrFactory.attrId(_)), calcWorkDuration) :::
    onUpdate.handlers(List(logAt.asWork,logAt.workDuration,logAt.entryOfWork).map(attrFactory.attrId(_)), calcEntryDuration) :::
    onUpdate.handlers(List(logAt.asEntry,logAt.confirmedOn,logAt.confirmedBy).map(attrFactory.attrId(_)), calcConfirmed)

  ////

  private def menuItem(key: VDomKey, caption: String)(activate: ()⇒Unit) =
    divNoWrap(key, divClickable(caption, Some{ ()⇒
      activate()
      popupOpened = ""
    }, withDivMargin("1",5,divBgColorHover("1",MenuItemHoverColor,withPadding("1",10,text("1",caption))))))

  private def toolbar(title:String): ChildPair[OfDiv] =
    paperWithMargin("toolbar", divWrapper("toolbar",None,Some("200px"),None,None,None,None,List(
      divWrapper("menu",None,None,None,None,Some("left"),None,
        fieldPopupBox("menu",
          List(btnMenu("menu",popupToggle("navMenu"))),
          if(popupOpened!="navMenu") Nil else List(
            menuItem("users","Users")(()⇒{currentVDom.relocate("/userList");popupOpened = ""}),
            menuItem("boats","Boats")(()⇒{currentVDom.relocate("/boatList");popupOpened = ""}),
            menuItem("entries","Entries")(()=>{currentVDom.relocate("/entryList");popupOpened = ""})
          )
        ) ::Nil
      ),
      divWrapper("1",Some("inline-block"),None,None,Some("50px"),None,None,
        withSidePadding("1",50,
          divAlignWrapper("1","left","middle",withSideMargin("1",10,text("title",title))::Nil))::Nil
      ),
      divWrapper("2",None,None,None,None,Some("right"),None,
        eventToolbarButtons()
      )
    )))


  def handlers =
    List(findEntry,findWorkByEntry,findBoat).flatMap(searchIndex.handlers(_)) :::
    List(
      logAt.durationTotal, logAt.asConfirmed, logAt.workDuration
    ).flatMap(factIndex.handlers(_)) :::
    List(
      logAt.asEntry, logAt.boat, logAt.confirmedOn, logAt.date, logAt.confirmedBy,
      logAt.dateFrom, logAt.dateTo, logAt.hideConfirmed,
      logAt.asWork, logAt.entryOfWork,
      logAt.workStart, logAt.workStop, logAt.workComment,
      logAt.asBoat, logAt.boatName
    ).flatMap{ attr⇒
      factIndex.handlers(attr) ::: alien.update(attr)
    } :::
    uiStrings.handlers(List(logAt.asBoat, logAt.boatName))(_(logAt.boatName)) :::
    alien.update(findAttrs.justIndexed) :::
    CoHandler(AttrCaption(logAt.boat))("Boat") ::
    CoHandler(AttrCaption(logAt.date))("Date") ::
    CoHandler(AttrCaption(logAt.durationTotal))("Total duration, hrs:min") ::
    CoHandler(AttrCaption(logAt.asConfirmed))("Confirmed") ::
    CoHandler(AttrCaption(logAt.confirmedBy))("Confirmed by") ::
    CoHandler(AttrCaption(logAt.confirmedOn))("Confirmed on") ::
    CoHandler(AttrCaption(logAt.workStart))("Start") ::
    CoHandler(AttrCaption(logAt.workStop))("Stop") ::
    CoHandler(AttrCaption(logAt.workDuration))("Duration, hrs:min") ::
    CoHandler(AttrCaption(logAt.workComment))("Comment") ::
    CoHandler(AttrCaption(logAt.boatName))("Name") ::
    CoHandler(AttrCaption(logAt.dateFrom))("Date From") ::
    CoHandler(AttrCaption(logAt.dateTo))("Date To") ::
    CoHandler(AttrCaption(logAt.hideConfirmed))("Hide Confirmed") ::
    CoHandler(ViewPath(""))(emptyView) ::
    CoHandler(ViewPath("/userList"))(userListView) ::
    CoHandler(ViewPath("/boatList"))(boatListView) ::
    CoHandler(ViewPath("/entryList"))(entryListView) ::
    CoHandler(ViewPath("/entryEdit"))(entryEditView) ::
    eventListHandlers :::
    calcHandlers :::
    CoHandler(SessionInstantAdded)(currentVDom.invalidate) ::
    CoHandler(TransientChanged)(currentVDom.invalidate) ::
    Nil
}

class FuelingAttrs(
  attr: AttrFactory,
  label: LabelFactory,
  objIdFactory: ObjIdFactory,
  asString: AttrValueType[String],
  asDuration: AttrValueType[Option[Duration]]
)(
  // 00 08 RF 24
  val asFueling: Attr[Obj] = label("8fc310bc-0ae7-4ad7-90f1-2dacdc6811ad"),
  //val meHoursStr: Attr[String] = attr("5415aa5e-efec-4f05-95fa-4954fee2dd2e", asString),
  val meHours: Attr[Option[Duration]] = attr("9be17c9f-6689-44ca-badf-7b55cc53a6b0", asDuration),
  val fuel: Attr[String] = attr("f29cdc8a-4a93-4212-bb23-b966047c7c4d", asString),
  val comment: Attr[String] = attr("2589cfd4-b125-4e4d-b3e9-9200690ddbc9", asString),
  val engineer: Attr[String] = attr("e5fe80e5-274a-41ab-b8b8-1909310b5a17", asString),
  val master: Attr[String] = attr("b85d4572-8cc5-42ad-a2f1-a3406352800a", asString),
  val time: Attr[String] = attr("df695468-c2bd-486c-9e58-f83da9566940", asString),
  val time00: ObjId = objIdFactory.toObjId("2b4c0bbf-fd24-4df8-b57a-29c26af11b23"),
  val time08: ObjId = objIdFactory.toObjId("a281aafc-b32d-4cf0-8599-eee8448c937d"),
  val time24: ObjId = objIdFactory.toObjId("f6bdcef8-179e-4da3-8c3d-ec7f51716ee6")
)

class FuelingItems(
  at: FuelingAttrs,
  findAttrs: FindAttrs,
  alienAttrs: AlienAccessAttrs,
  filterAttrs: FilterAttrs,
  nodeAttrs: NodeAttrs,
  factIndex: FactIndex,
  searchIndex: SearchIndex,
  alien: Alien,
  filters: Filters,
  onUpdate: OnUpdate,
  attrFactory: AttrFactory,
  dbWrapType: WrapType[ObjId],
  validationFactory: ValidationFactory
)(
  val fuelingByFullKey: SearchByLabelProp[ObjId] = searchIndex.create(at.asFueling,filterAttrs.filterFullKey),
  val times: List[ObjId] = List(at.time00,at.time08,at.time24)
) extends CoHandlerProvider {
  def handlers =
    CoHandler(GetValue(dbWrapType, at.time)){ (obj,innerObj)⇒
      if(innerObj.data == at.time00) "00:00" else
      if(innerObj.data == at.time08) "08:00" else
      if(innerObj.data == at.time24) "24:00" else throw new Exception(s"? ${innerObj.data}")
    } ::
    CoHandler(AttrCaption(at.meHours))("ME Hours.Min") ::
    CoHandler(AttrCaption(at.fuel))("Fuel rest/quantity") ::
    CoHandler(AttrCaption(at.comment))("Comment") ::
    CoHandler(AttrCaption(at.engineer))("Engineer") ::
    CoHandler(AttrCaption(at.master))("Master") ::
    searchIndex.handlers(fuelingByFullKey) :::
    //List(at.meHours).flatMap{ attr ⇒ factIndex.handlers(attr) } :::
    List(
      at.asFueling, at.meHours/*Str*/, at.fuel, at.comment, at.engineer, at.master
    ).flatMap{ attr ⇒
      factIndex.handlers(attr) ::: alien.update(attr)
    }/* :::
    onUpdate.handlers(List(at.asFueling,at.meHoursStr).map(attrFactory.attrId(_)), {
      (on: Boolean, fueling: Obj)⇒
      fueling(at.meHours) = if(!on) None else {
        val Time = """(\d+)\:(\d+)""".r
        fueling(at.meHoursStr) match {
          case Time(h,m) ⇒ Some(Duration.ofMinutes(Integer.parseUnsignedInt(h)*60+Integer.parseUnsignedInt(m)))
          case _ ⇒ None
        }
      }
    })*/
/*
else if(!fuelingItems.meHoursIsInc(entry)){
                        List(text("1",s"ME times shold increase"))
                      }
* */
  def fueling(entry: Obj, time: ObjId, wrapForEdit: Boolean) =
    filters.lazyLinkingObj(fuelingByFullKey,List(entry(nodeAttrs.objId),time),wrapForEdit)
  def validation(entry: Obj): List[ValidationState] = {
    val fuelingList = times.map(time⇒fueling(entry, time, wrapForEdit = false))
    fuelingList.flatMap { obj ⇒
      validationFactory.need[String](obj,at.fuel,v⇒if(v.isEmpty) Some("") else None) :::
      validationFactory.need[String](obj,at.engineer,v⇒if(v.isEmpty) Some("") else None) :::
      validationFactory.need[String](obj,at.master,v⇒if(v.isEmpty) Some("") else None)
    } :::
    fuelingList.sliding(2).toList.flatMap{ fuelingPair ⇒ fuelingPair.map(_(at.meHours)) match {
      case Some(a) :: Some(b) :: Nil if a.compareTo(b) <= 0 ⇒ Nil
      case _ ⇒ validationFactory.need[Option[Duration]](fuelingPair.head,at.meHours,v⇒Some("to increase"))
    }}
  }
}




/*
case class RegItem[R](index: Int)(create: ()⇒R){
    var value: Option[R] = None
    def apply() = {
        if(value.isEmpty) value = Some(create())
        value.get
    }
}
class RegList {
    val reg = scala.collection.mutable.ArrayBuffer[RegItem[_]]()
    def value[R](create: ⇒R): RegItem[R] = {
        val res = RegItem(reg.size)(()=>create)
        reg += res
        res
    }
}

*/



