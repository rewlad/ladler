
package ee.cone.base.test_loots // demo

import java.time.{Duration, Instant, LocalTime, ZonedDateTime}
import java.util.UUID

import ee.cone.base.connection_api._
import ee.cone.base.db._
import ee.cone.base.flexlayout.{FlexTags, MaxVisibleLines, Priority}
import ee.cone.base.material._
import ee.cone.base.util.Never
import ee.cone.base.vdom.Types.VDomKey
import ee.cone.base.vdom._

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
  val locationOfEntry: Attr[Obj] = attr("6ccde3e9-5522-4adb-9770-a1301506afd7",asObj),

  val dateFrom: Attr[Option[Instant]] = attr("6e260496-9534-4ca1-97f6-6b234ef93a55", asInstant),
  val dateTo: Attr[Option[Instant]] = attr("7bd7e2cb-d7fd-4b0f-b88b-b1e70dd609a1", asInstant),
  val hideConfirmed: Attr[Boolean] = attr("d49d29e0-d797-413e-afc6-2f62b06840ca", asBoolean),

  val asWork: Attr[Obj] = label("5cce1cf2-1793-4e54-8523-c810f7e5637a"),
  val workStart: Attr[Option[LocalTime]] = attr("41d0cbb8-56dd-44da-96a6-16dcc352ce99", asLocalTime),
  val workStop: Attr[Option[LocalTime]] = attr("5259ef2d-f4de-47b7-bc61-0cfe33cb58d3", asLocalTime),
  val workDuration: Attr[Option[Duration]] = attr("547917b2-7bb6-4240-9fba-06248109d3b6", asDuration),
  val workComment: Attr[String] = attr("5cec443e-8396-4d7b-99c5-422a67d4b2fc", asString),
  val entryOfWork: Attr[Obj] = attr("119b3788-e49a-451d-855a-420e2d49e476", asObj),
  val workDate: Attr[Option[Instant]] = attr("e6df8fe4-86c7-4255-b93e-86de9a5a5d25", asInstant),

  val asBoat: Attr[Obj] = label("c6b74554-4d05-4bf7-8e8b-b06b6f64d5e2"),
  val boatName: Attr[String] = attr("7cb71f3e-e8c9-4f11-bfb7-f1d0ff624f09", asString),
  val locationOfBoat: Attr[Obj] = attr("0924571d-e8c6-4d62-ac50-2c4404e7fc6e",asObj)
)

class TestComponent(
  nodeAttrs: NodeAttrs,
  listAttrs: ObjSelectionAttributes,
  logAt: BoatLogEntryAttributes,
  userAttrs: UserAttrs,
  fuelingAttrs: FuelingAttrs,
  validationAttrs: ValidationAttributes,
  handlerLists: CoHandlerLists,
  attrFactory: AttrFactory,
  findNodes: FindNodes,
  mainTx: CurrentTx[MainEnvKey],
  alien: Alien,
  onUpdate: OnUpdate,
  divTags: Tags,
  currentVDom: CurrentVDom,
  searchIndex: SearchIndex,
  factIndex: FactIndex,
  htmlTable: TableTags,
  users: Users,
  fuelingItems: FuelingItems,
  objIdFactory: ObjIdFactory,
  validationFactory: ValidationFactory,
  asDuration: AttrValueType[Option[Duration]],
  asInstant: AttrValueType[Option[Instant]],
  asLocalTime: AttrValueType[Option[LocalTime]],
  asBigDecimal: AttrValueType[Option[BigDecimal]],
  asDBObj: AttrValueType[Obj],
  asString: AttrValueType[String],
  asUUID: AttrValueType[Option[UUID]],
  uiStrings: UIStrings,
  mandatory: Mandatory,
  zoneIds: ZoneIds,
  itemListOrderingFactory: ItemListOrderingFactory,
  objOrderingFactory: ObjOrderingFactory,
  errorAttributes: ErrorAttributes,
  errors: Errors,
  listedFactory: IndexedObjCollectionFactory,
  filterObjFactory: FilterObjFactory,
  editing: Editing,
  inheritAttrRule: InheritAttrRule,
  tableUtils: MaterialDataTableUtils,
  fields: Fields,
  fieldAttributes: FieldAttributes,
  style: TagStyles,
  materialTags: MaterialTags,
  flexTags: FlexTags,
  optionTags: OptionTags,
  buttonTags: ButtonTags,
  tableUtilTags: TableUtilTags
)(
  val findEntry: SearchByLabelProp[Obj] = searchIndex.create(logAt.asEntry, logAt.locationOfEntry),
  val findWorkByEntry: SearchByLabelProp[Obj] = searchIndex.create(logAt.asWork, logAt.entryOfWork),
  val findBoat: SearchByLabelProp[Obj] = searchIndex.create(logAt.asBoat, logAt.locationOfBoat)
) extends CoHandlerProvider {
  import divTags._
  import materialTags._
  import buttonTags._
  import flexTags._
  import htmlTable._
  import uiStrings.caption
  import tableUtils._
  import fields.field
  import fieldAttributes._
  import optionTags._
  import tableUtilTags._

  def eventSource = handlerLists.single(SessionEventSource, ()⇒Never())

  private def mCell(key: VDomKey, minWidth: Int)(children: CellContentVariant ⇒ List[ChildPair[OfDiv]]) =
    cell(key, MinWidth(minWidth))(children)
  private def mCell(key: VDomKey, minWidth: Int, priority: Int)(children: CellContentVariant ⇒ List[ChildPair[OfDiv]]) =
    cell(key, MinWidth(minWidth), Priority(priority))(children)
  private def divAlignWrapper(key:VDomKey, outerStyles: TagStyle*)(innerStyles: TagStyle*)(children:List[ChildPair[OfDiv]])=
    div(key,Seq(style.displayTable,style.widthAll)++outerStyles:_*)(List(
      div("1",Seq(style.displayCell,style.widthAll,style.heightAll)++innerStyles:_*)(children)
    ))

  private def entryListView(pf: String) = wrap{ ()=>{
    val editable = true //todo roles
    val filterObj = filterObjFactory.create(List(attrFactory.attrId(logAt.asEntry)))
    val filterList: List[Obj⇒Boolean] = {
      val value = filterObj(logAt.boat)(nodeAttrs.objId)
      if(value.nonEmpty) List((obj:Obj) ⇒ obj(logAt.boat)(nodeAttrs.objId)==value) else Nil
    } ::: {
      val value = filterObj(logAt.dateFrom)
      if(value.nonEmpty) List((obj:Obj) ⇒ obj(logAt.date).forall((v:Instant) ⇒ v==value.get || v.isAfter(value.get))) else Nil
    } ::: {
      val value = filterObj(logAt.dateTo)
      if(value.nonEmpty) List((obj:Obj) ⇒ obj(logAt.date).forall((v:Instant) ⇒ v==value.get || v.isBefore(value.get))) else Nil
    } ::: {
      val value = filterObj(logAt.hideConfirmed)
      if(value) List((obj:Obj) ⇒ !obj(logAt.asConfirmed)(aNonEmpty) ) else Nil
    }
//logAt.dateFrom, logAt.dateTo, logAt.hideConfirmed,
    val listed = listedFactory.create(findEntry,users.world)
    val itemList = createItemList(listed,filterObj,filterList,editable)
    val itemListOrdering = itemListOrderingFactory.itemList(filterObj)
    def go(entry: Obj) = currentVDom.relocate(s"/entryEdit/${entry(aObjIdStr)}")


    List( //class LootsBoatLogList
      toolbar("Entry List"),
      div("1", style.maxWidth(2056), style.marginLeftAuto, style.marginRightAuto)(List(paperTable("table")(
        List(inset("controlPanel",controlPanel(
          List(flexGrid("controlGrid1")(List(
            flexItem("1a",150,Some(200))(_⇒field(filterObj, logAt.boat, showLabel = true)),
            flexItem("2a",150,Some(200))(_⇒field(filterObj, logAt.dateFrom, showLabel = true)),
            flexItem("3a",150,Some(200))(_⇒field(filterObj, logAt.dateTo, showLabel = true)),
            flexItem("4a",150,Some(200))(_⇒List(
              divAlignWrapper("1",style.height(72))(style.alignBottom,style.padding(10))(
                field(filterObj, logAt.hideConfirmed, showLabel = true)
              )
            ))
          ))),
          addRemoveControlViewBase(itemList)(go)
        ))),
        List(
          row("row",MaxVisibleLines(2),IsHeader)(
            selectAllGroup(itemList) :::
            List(
              group("2_grp",MinWidth(150),Priority(3),style.alignCenter)(Nil),
              mCell("2",100)(_=>sortingHeader(itemListOrdering,logAt.boat)),
              mCell("3",150)(_=>sortingHeader(itemListOrdering,logAt.date)),
              mCell("4",180)(_=>sortingHeader(itemListOrdering,logAt.durationTotal)),
              mCell("5",100)(_=>sortingHeader(itemListOrdering,logAt.asConfirmed)),
              mCell("6",150)(_=>sortingHeader(itemListOrdering,logAt.confirmedBy)),
              mCell("7",150)(_=>sortingHeader(itemListOrdering,logAt.confirmedOn))
            ) :::
            editAllGroup()
          )
        ) :::
        itemList.list.sorted(itemListOrdering.compose(creationTimeOrdering)).map{ (entry:Obj)=>
          val entrySrcId = entry(aObjIdStr)
          row(entrySrcId, MaxVisibleLines(2) :: toggledSelectedRow(entry):_*)(
            selectRowGroup(entry) :::
            List(
              group("2_grp", MinWidth(150), Priority(3), style.alignCenter)(Nil),
              mCell("2",100)(showLabel=>field(entry, logAt.boat, showLabel)),
              mCell("3",150)(showLabel=>field(entry, logAt.date, showLabel)),
              mCell("4",180)(showLabel=>field(entry, logAt.durationTotal, showLabel)),
              mCell("5",100)(_=>
               {
                  val confirmed = entry(logAt.asConfirmed)
                  if(confirmed(aNonEmpty))
                    List(materialChip("1","CONFIRMED")(None))
                  else Nil
               }
              ),
              mCell("6",150)(showLabel=>field(entry, logAt.confirmedBy, showLabel)),
              mCell("7",150)(showLabel=>field(entry, logAt.confirmedOn, showLabel))
            ) :::
            editRowGroupBase(on=true)(()⇒go(entry))
          )
        }
      )))
    )
  }}

  private def boatOptions(obj: Obj) =
    listedFactory.create(findBoat, users.world).toList

  private def entryEditView(pf: String) = wrap { () =>
    val entryObj = findNodes.whereObjId(objIdFactory.toObjId(pf.tail))(logAt.asEntry)
    val isConfirmed = entryObj(logAt.asConfirmed)(aNonEmpty)
    val validationStates = if(isConfirmed) Nil else
      fuelingItems.validation(entryObj) :::
      validationFactory.need[Obj](entryObj,logAt.boat,v⇒if(!v(aNonEmpty)) Some("") else None) :::
      validationFactory.need[Option[Instant]](entryObj,logAt.date,v⇒if(v.isEmpty) Some("") else None)
    val validationContext = validationFactory.context(validationStates)
    val entry = if(isConfirmed) entryObj else validationContext.wrap(alien.wrapForUpdate(entryObj)) /*todo roles*/
    val entryIdStr = entry(aObjIdStr)

    List(
      toolbar("Entry Edit"),
      div("1",style.maxWidth(1200), style.marginLeftAuto, style.marginRightAuto)(List(
      paperWithMargin(s"$entryIdStr-1",
        flexGrid("flexGridEdit1")(List(
          flexItem("1",500,None)(_⇒List(
            flexGrid("FlexGridEdit11")(List(
              flexItem("boat1",100,None)(_⇒field(entry,logAt.boat, showLabel = true)),
              flexItem("date",150,None)(_⇒field(entry, logAt.date, showLabel = true)),
              flexItem("dur",170,None)(_⇒List(divAlignWrapper("1",style.heightAll)(style.alignLeft,style.alignMiddle)(
                field(entry,logAt.durationTotal, showLabel = true, EditableFieldOption(false))
              )))
            ))
          )),
          flexItem("2",500,None)(_⇒List(
            flexGrid("flexGridEdit12")(
              (if(!isConfirmed) Nil else List(
                flexItem("conf_by",150,None)(_⇒field(entry, logAt.confirmedBy, showLabel = true)),
                flexItem("conf_on",150,None)(_⇒field(entry, logAt.confirmedOn, showLabel = true))
              )) :::
              List(
                flexItem("conf_do",150,None)(_⇒List(
                  divAlignWrapper("1",style.height(72))(style.alignRight,style.alignBottom)(
                    if(isConfirmed) {
                      val entry = alien.wrapForUpdate(entryObj)
                      List(
                        raisedButton("reopen", "Reopen") { () ⇒
                          entry(logAt.confirmedOn) = None
                          entry(logAt.confirmedBy) = findNodes.noNode
                        }
                      )
                    }
                    else if(validationStates.nonEmpty){
                      val state = validationStates.head
                      List(alert("1",state.text))
                    }
                    else {
                      val user = eventSource.mainSession(userAttrs.authenticatedUser)
                      if(!user(aNonEmpty)) List(alert("1",s"User required"))
                      else List(raisedButton("confirm","Confirm"){()⇒
                        entry(logAt.confirmedOn) = Option(Instant.now())
                        entry(logAt.confirmedBy) = user
                      })
                    }
                  )
                ))
              )
            )
          )))
        )
      ))),

      div("2",style.maxWidth(1200), style.marginLeftAuto, style.marginRightAuto)(List(
        entryEditFuelScheduleView(entry, validationStates)
      )),
      div("3",style.maxWidth(1200), style.marginLeftAuto, style.marginRightAuto)(List(
        entryEditWorkListView(entry)
      ))



    )
  }


  def entryEditFuelScheduleView(entry: Obj, validationStates: List[ValidationState]): ChildPair[OfDiv] = {
    //val entryIdStr = entry(alien.objIdStr)
    val filterObj = filterObjFactory.create(List(entry(nodeAttrs.objId)))
    val deferSend = DeferSendFieldOption(validationStates.size > 2)
    val validationContext = validationFactory.context(validationStates)

    def fuelingRowView(time: ObjId, isRF: Boolean) = {
      val fueling = validationContext.wrap(
        if(isRF) entry else fuelingItems.fueling(entry, time, wrapForEdit=entry(aIsEditing))
      )
      val timeStr = if(isRF) "RF" else findNodes.whereObjId(time)(fuelingAttrs.time)
      row(timeStr,toggledRow(filterObj,time):_*)(List(
        mCell("1",100,3)(showLabel=>
          if(isRF) List(text("1","Passed"))
          else field(findNodes.whereObjId(time), fuelingAttrs.time, showLabel, EditableFieldOption(false))
        ),
        mCell("2",150,1)(showLabel=>
          if(isRF) List(text("1","Received Fuel"))
          else field(fueling, fuelingAttrs.meHours, showLabel)
          /*text("c",if(fueling(fuelingAttrs.meHours).nonEmpty) "+" else "-" ) ::
            strField(fueling, fuelingAttrs.meHoursStr, showLabel, deferSend = false)*/
        ),
        mCell("3",100,1)(showLabel=>
          field(fueling, fuelingAttrs.fuel, showLabel, deferSend)
        ),
        mCell("4",250,3)(showLabel=>
          field(fueling, fuelingAttrs.comment, showLabel, deferSend)
        ),
        mCell("5",150,2)(showLabel=>
          field(fueling, fuelingAttrs.engineer, showLabel, deferSend)
        ),
        mCell("6",150,2)(showLabel=>
          if(isRF) Nil
          else field(fueling, fuelingAttrs.master, showLabel, deferSend)
        )
      ))
    }
    paperTable("dtTableEdit1")(Nil,List(
      row("row",IsHeader)(List(
        mCell("1",100,3)(_=>List(text("1","Time"))),
        mCell("2",150,1)(_=>List(text("1",caption(fuelingAttrs.meHours)))),
        mCell("3",100,1)(_=>List(text("1",caption(fuelingAttrs.fuel)))),
        mCell("4",250,3)(_=>List(text("1",caption(fuelingAttrs.comment)))),
        mCell("5",150,2)(_=>List(text("1",caption(fuelingAttrs.engineer)))),
        mCell("6",150,2)(_=>List(text("1",caption(fuelingAttrs.master))))
      )),
      fuelingRowView(fuelingAttrs.time00,isRF = false),
      fuelingRowView(fuelingAttrs.time08,isRF = false),
      fuelingRowView(objIdFactory.noObjId,isRF = true),
      fuelingRowView(fuelingAttrs.time24,isRF = false)
    ))
  }

  def entryEditWorkListView(entry: Obj): ChildPair[OfDiv] = {
    val entryIdStr = entry(aObjIdStr)
    val filterObj = filterObjFactory.create(List(entry(nodeAttrs.objId),attrFactory.attrId(logAt.asWork)))
    val listed = listedFactory.create(findWorkByEntry,entry)
    val workList = createItemList(listed,filterObj,Nil,entry(aIsEditing))
    paperTable("dtTableEdit2")(
      controlPanel(Nil,addRemoveControlView(workList)),
      List(
        row("row",IsHeader)(
          selectAllGroup(workList) :::
          List(
            group("2_group",MinWidth(150))(Nil),
            mCell("2",100)(_=>header(logAt.workStart)),
            mCell("3",100)(_=>header(logAt.workStop)),
            mCell("4",150)(_=>header(logAt.workDuration)),
            mCell("5",250,3)(_=>header(logAt.workComment))
          ) :::
          editAllGroup()
        )
      ) :::
      workList.list.sorted(creationTimeOrdering.reverse).map { (work: Obj) =>
        val workSrcId = work(aObjIdStr)
        row(workSrcId, toggledSelectedRow(work):_*)(
          selectRowGroup(work) :::
          List(
            group("2_group",MinWidth(150))(Nil),
            mCell("2",100)(showLabel=>
              field(work, logAt.workStart, showLabel)
            ),
            mCell("3",100)(showLabel=>
              field(work, logAt.workStop, showLabel)
            ),
            mCell("4",150)(showLabel=>
              field(work, logAt.workDuration, showLabel, EditableFieldOption(false))
            ),
            mCell("5",250,3)(showLabel=>
              field(work, logAt.workComment, showLabel)
            )
          ) :::
          editRowGroup(workList, work)
        )
      }
    )
  }

  private def boatListView(pf: String) = wrap { () =>
    val filterObj = filterObjFactory.create(List(attrFactory.attrId(logAt.asBoat)))
    val listed = listedFactory.create(findBoat, users.world)
    val itemList = createItemList[Obj](listed, filterObj, Nil, editable=true) //todo roles
    val itemListOrdering = itemListOrderingFactory.itemList(filterObj)
    List(
      toolbar("Boats"),
      div("maxWidth", style.maxWidth(600), style.marginLeftAuto, style.marginRightAuto)(List(paperTable("table")(
        controlPanel(Nil, addRemoveControlView(itemList)),
        List(
          row("head",IsHeader)(
            selectAllGroup(itemList) :::
            List(
              group("2_group",MinWidth(50))(Nil),
              mCell("1",250)(_⇒sortingHeader(itemListOrdering,logAt.boatName))
            ) :::
            editAllGroup()
          )
        ) :::
        itemList.list.sorted(itemListOrdering.compose(creationTimeOrdering)).map{boat ⇒
          val srcId = boat(aObjIdStr)
          row(srcId,toggledSelectedRow(boat):_*)(
            selectRowGroup(boat) :::
            List(
              group("2_group",MinWidth(50))(Nil),
              mCell("1",250)(showLabel⇒field(boat, logAt.boatName, showLabel))
            ) :::
            editRowGroup(itemList, boat)
          )
        }
      )))
    )
  }

  //// calculations

  private def calcWorkDuration(on: Boolean, work: Obj): Unit = {
    work(logAt.workDuration) = if(!on) None else {
      val date: ZonedDateTime = work(logAt.workDate).get.atZone(zoneIds.zoneId)
      if(date.toLocalTime != LocalTime.of(0,0)) None else {
        val start: ZonedDateTime = date.`with`(work(logAt.workStart).get)
        val stopTime: LocalTime = work(logAt.workStop).get
        val stop: ZonedDateTime =
          if(stopTime == LocalTime.of(0,0)) date.plusDays(1)
          else date.`with`(stopTime)
        if(start.isBefore(stop)) Option(Duration.between(start, stop)) else None
      }


      /*
      if(start.isBefore(stop)) Option(Duration.between(start, stop))
      else if(LocalTime.of(0,0)==start && start==stop) Option(Duration.ofDays(1))
      else None*/
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
    inheritAttrRule(logAt.date, logAt.workDate, findWorkByEntry) :::
    onUpdate.handlers(List(logAt.asWork,logAt.workStart,logAt.workStop,logAt.workDate), Nil)(calcWorkDuration) :::
    onUpdate.handlers(List(logAt.asWork,logAt.workDuration,logAt.entryOfWork), Nil)(calcEntryDuration) :::
    onUpdate.handlers(List(logAt.asEntry,logAt.confirmedOn,logAt.confirmedBy), Nil)(calcConfirmed)

  ////

  def handlers =
    CoHandler(MenuItems)(()⇒List(
      option("boats","Boats")(()⇒currentVDom.relocate("/boatList")),
      option("entries","Entries")(()⇒currentVDom.relocate("/entryList"))
    )) ::
    CoHandler(AttrValueOptions(logAt.boat))(boatOptions) ::
    List(findEntry,findWorkByEntry,findBoat).flatMap(searchIndex.handlers) :::
    List(
      logAt.durationTotal, logAt.asConfirmed, logAt.workDuration, logAt.workDate
    ).flatMap(factIndex.handlers(_)) :::
    List(
      logAt.asEntry, logAt.boat, logAt.confirmedOn, logAt.date, logAt.confirmedBy,
      logAt.dateFrom, logAt.dateTo, logAt.hideConfirmed, logAt.locationOfEntry,
      logAt.asWork, logAt.entryOfWork,
      logAt.workStart, logAt.workStop, logAt.workComment,
      logAt.asBoat, logAt.boatName, logAt.locationOfBoat
    ).flatMap(alien.update(_)) :::
    uiStrings.captions(logAt.asEntry, Nil)(_⇒"") :::
    uiStrings.captions(logAt.asWork, Nil)(_⇒"") :::
    uiStrings.captions(logAt.asBoat, logAt.boatName::Nil)(_(logAt.boatName)) :::
    CoHandler(AttrCaption(logAt.asEntry))("Entry") ::
    CoHandler(AttrCaption(logAt.locationOfEntry))("Realm") ::
    CoHandler(AttrCaption(logAt.boat))("Boat") ::
    CoHandler(AttrCaption(logAt.date))("Date") ::
    CoHandler(AttrCaption(logAt.durationTotal))("Total duration, hrs:min") ::
    CoHandler(AttrCaption(logAt.asConfirmed))("Confirmed") ::
    CoHandler(AttrCaption(logAt.confirmedBy))("Confirmed by") ::
    CoHandler(AttrCaption(logAt.confirmedOn))("Confirmed on") ::
    CoHandler(AttrCaption(logAt.asWork))("Work") ::
    CoHandler(AttrCaption(logAt.entryOfWork))("Entry") ::
    CoHandler(AttrCaption(logAt.workStart))("Start") ::
    CoHandler(AttrCaption(logAt.workStop))("Stop") ::
    CoHandler(AttrCaption(logAt.workDuration))("Duration, hrs:min") ::
    CoHandler(AttrCaption(logAt.workComment))("Comment") ::
    CoHandler(AttrCaption(logAt.asBoat))("Boat") ::
    CoHandler(AttrCaption(logAt.boatName))("Name") ::
    CoHandler(AttrCaption(logAt.locationOfBoat))("Realm") ::
    CoHandler(AttrCaption(logAt.dateFrom))("Date From") ::
    CoHandler(AttrCaption(logAt.dateTo))("Date To") ::
    CoHandler(AttrCaption(logAt.hideConfirmed))("Hide Confirmed") ::
    CoHandler(ViewPath("/boatList"))(boatListView) ::
    CoHandler(ViewPath("/entryList"))(entryListView) ::
    CoHandler(ViewPath("/entryEdit"))(entryEditView) ::
    calcHandlers :::
    mandatory(logAt.locationOfBoat, logAt.boatName, mutual = true) :::
    mandatory(logAt.entryOfWork, logAt.workDuration, mutual = true)
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



