package ee.cone.base.test_loots

import java.nio.ByteBuffer
import java.time.format.DateTimeFormatter
import java.time._
import java.util.UUID

import ee.cone.base.util.Single
import ee.cone.base.connection_api._
import ee.cone.base.db._
import ee.cone.base.server.SenderOfConnection
import ee.cone.base.util.{Bytes, Never}
import ee.cone.base.vdom.Types.VDomKey
import ee.cone.base.vdom._

import scala.collection.mutable

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

class DurationValueConverter(
  val valueType: AttrValueType[Option[Duration]], inner: RawConverter
) extends RawValueConverterImpl[Option[Duration]] {
  def convertEmpty() = None
  def convert(valueA: Long, valueB: Long) = Option(Duration.ofSeconds(valueA,valueB))
  def convert(value: String) = Never()
  def toBytes(preId: ObjId, value: Value, finId: ObjId) =
    if(value.nonEmpty) inner.toBytes(preId, value.get.getSeconds, value.get.getNano, finId) else Array()
}

class InstantValueConverter(
  val valueType: AttrValueType[Option[Instant]], inner: RawConverter
) extends RawValueConverterImpl[Option[Instant]] {
  def convertEmpty() = None
  def convert(valueA: Long, valueB: Long) = Option(Instant.ofEpochSecond(valueA,valueB))
  def convert(value: String) = Never()
  def toBytes(preId: ObjId, value: Value, finId: ObjId) =
    if(value.nonEmpty) inner.toBytes(preId, value.get.getEpochSecond, value.get.getNano, finId) else Array()
}

class TestAttributes(
  attr: AttrFactory,
  label: LabelFactory,
  asString: AttrValueType[String]
)(
  val caption: Attr[String] = attr("2aec9be5-72b4-4983-b458-4f95318bfd2a", asString)
)

class BoatLogEntryAttributes(
  attr: AttrFactory,
  label: LabelFactory,

  asObj: AttrValueType[Obj],
  asString: AttrValueType[String],
  asInstant: AttrValueType[Option[Instant]],
  asDuration: AttrValueType[Option[Duration]]
)(
  val asEntry: Attr[Obj] = label("21f5378d-ee29-4603-bc12-eb5040287a0d"),
  val boat: Attr[Obj] = attr("b65d201b-8b83-41cb-85a1-c0cb2b3f8b18", asObj),
  val date: Attr[Option[Instant]] = attr("7680a4db-0a6a-45a3-bc92-c3c60db42ef9", asInstant),
  val durationTotal: Attr[Option[Duration]] = attr("6678a3d1-9472-4dc5-b79c-e43121d2b704", asDuration),
  val asConfirmed: Attr[Obj] = label("c54e4fd2-0989-4555-a8a5-be57589ff79d"),
  val confirmedBy: Attr[Obj] = attr("36c892a2-b5af-4baa-b1fc-cbdf4b926579", asObj),
  val confirmedOn: Attr[Option[Instant]] = attr("b10de024-1016-416c-8b6f-0620e4cad737", asInstant), //0x6709

  val log00Date: Attr[Option[Instant]] = attr("9be17c9f-6689-44ca-badf-7b55cc53a6b0", asInstant),
  val log00Fuel: Attr[String] = attr("f29cdc8a-4a93-4212-bb23-b966047c7c4d", asString),
  val log00Comment: Attr[String] = attr("2589cfd4-b125-4e4d-b3e9-9200690ddbc9", asString),
  val log00Engineer: Attr[String] = attr("e5fe80e5-274a-41ab-b8b8-1909310b5a17", asString),
  val log00Master: Attr[String] = attr("b85d4572-8cc5-42ad-a2f1-a3406352800a", asString),
  val log08Date: Attr[Option[Instant]] = attr("6e5f46e6-0aca-4863-b010-52ec04979b84", asInstant),
  val log08Fuel: Attr[String] = attr("8740c331-3080-4080-b09e-02d2a4d6b93e", asString),
  val log08Comment: Attr[String] = attr("32222649-c14d-4a50-b420-f748df40f1d5", asString),
  val log08Engineer: Attr[String] = attr("06230e9b-ba76-42a6-be0f-a221dea7924c", asString),
  val log08Master: Attr[String] = attr("6f45d7aa-1c92-416f-93d0-45b035997b86", asString),
  val logRFFuel: Attr[String] = attr("2954c1c8-6335-4654-8367-22bae26a19f3", asString),
  val logRFComment: Attr[String] = attr("eae6e221-fde1-4d1f-aa64-7e0cf56e128a", asString),
  val logRFEngineer: Attr[String] = attr("1c4dc625-1bbd-4d67-b54a-8562d240c2fc", asString), // 0x670A,0x670E,0x670F
  val log24Date: Attr[Option[Instant]] = attr("69b1b080-1094-4780-9898-4fb15f634c27", asInstant),
  val log24Fuel: Attr[String] = attr("76053737-d75d-4b7b-b0a5-f2cadae61f6f", asString),
  val log24Comment: Attr[String] = attr("6ec9cda4-78d9-4964-8be2-9b596e355525", asString),
  val log24Engineer: Attr[String] = attr("735a1265-81e0-4ade-83a3-99bf86702925", asString),
  val log24Master: Attr[String] = attr("5930f78d-285d-499e-a665-387792f49807", asString), // 0x671F

  val asWork: Attr[Obj] = label("5cce1cf2-1793-4e54-8523-c810f7e5637a"),
  val workStart: Attr[Option[Instant]] = attr("41d0cbb8-56dd-44da-96a6-16dcc352ce99", asInstant),
  val workStop: Attr[Option[Instant]] = attr("5259ef2d-f4de-47b7-bc61-0cfe33cb58d3", asInstant),
  val workDuration: Attr[Option[Duration]] = attr("547917b2-7bb6-4240-9fba-06248109d3b6", asDuration),
  val workComment: Attr[String] = attr("5cec443e-8396-4d7b-99c5-422a67d4b2fc", asString),
  val entryOfWork: Attr[Obj] = attr("119b3788-e49a-451d-855a-420e2d49e476", asObj),

  val asBoat: Attr[Obj] = label("c6b74554-4d05-4bf7-8e8b-b06b6f64d5e2")
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
  nodeAttrs: NodeAttrs, findAttrs: FindAttrs,
  filterAttrs: FilterAttrs, at: TestAttributes, logAt: BoatLogEntryAttributes,
  userAttrs: UserAttrs,
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
  htmlTableWithControl: HtmlTableWithControl,
  users: Users
)(
  val findEntry: SearchByLabelProp[String] = searchIndex.create(logAt.asEntry, findAttrs.justIndexed),
  val findWorkByEntry: SearchByLabelProp[Obj] = searchIndex.create(logAt.asWork, logAt.entryOfWork)
) extends CoHandlerProvider {
  import tags._
  import materialTags._
  import flexTags._
  import htmlTableWithControl._
  import findAttrs.nonEmpty
  private def eventSource = handlerLists.single(SessionEventSource, ()⇒Never())

  private def toAlienText[Value](obj: Obj, attr: Attr[Value], valueToText: Value⇒String, label: String, showLabel: Boolean): List[ChildPair[OfDiv]] =
    if(!obj(nonEmpty)) Nil
    else if(!showLabel)
      List(text("1",valueToText(obj(attr))))
    else
      List(labeledText("1",valueToText(obj(attr)),label))

  private def strField(obj: Obj, attr: Attr[String], editable: Boolean, label: String, showLabel: Boolean): List[ChildPair[OfDiv]] = {
    val visibleLabel = if(showLabel) label else ""
    if(editable) List(textInput("1", visibleLabel, obj(attr), obj(attr) = _))
    else List(labeledText("1", obj(attr), visibleLabel))
  }

  private def booleanField(obj: Obj, attr: Attr[Boolean], editable: Boolean): List[ChildPair[OfDiv]] = {
    if(!editable) ???
    else List(checkBox("1", obj(attr), obj(attr)=_))
  }

  private def durationField(obj: Obj, attr: Attr[Option[Duration]], label:String, showLabel:Boolean): List[ChildPair[OfDiv]] = {
    toAlienText[Option[Duration]](
      obj, attr,
      v ⇒ v.map(x => x.abs.toHours+"h:"+x.abs.minusHours(x.abs.toHours).toMinutes.toString+"m").getOrElse(""),
      label, showLabel
    )
  }

  private def dateField(obj: Obj, attr: Attr[Option[Instant]], editable: Boolean, label:String, showLabel:Boolean): List[ChildPair[OfDiv]] = {
    val visibleLabel = if(showLabel) label else ""
    if(editable) List(timeInput("1", visibleLabel, obj(attr), obj(attr) = _))
    else {
      val dateStr = obj(attr).map{ v ⇒
        val date = LocalDate.from(v.atZone(ZoneId.of("UTC")))
        val formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy")
        date.format(formatter)
      }.getOrElse("")
      labeledText("1", label, dateStr) :: Nil
    }
  }

  private def timeField(obj: Obj, attr: Attr[Option[Instant]], editable: Boolean, label:String, showLabel:Boolean): List[ChildPair[OfDiv]] = {
    val visibleLabel = if(showLabel) label else ""
    if(editable) List(timeInput("1",visibleLabel,obj(attr),obj(attr)=_))
    else ???
  }

  private def objField(obj: Obj, attr: Attr[Obj], editable: Boolean,label:String,showLabel:Boolean): List[ChildPair[OfDiv]] ={
    toAlienText[Obj](obj,attr,v⇒if(v(nonEmpty)) v(at.caption) else "",label,showLabel)
  }

  ////

  private def emptyView(pf: String) =
    tags.root(List(tags.text("text", "Loading...")))

  private def wrapDBView(view: ()=>VDomValue): VDomValue =
    eventSource.incrementalApplyAndView { () ⇒
      val user = eventSource.mainSession(userAttrs.authenticatedUser)
      if(user(nonEmpty)) {
        val startTime = System.currentTimeMillis
        val res = view()
        val endTime = System.currentTimeMillis
        currentVDom.until(endTime + (endTime - startTime) * 10)
        res
      } else loginView()
    }

  private def paperWithMargin(key: VDomKey, child: ChildPair[OfDiv]) =
    withMargin(key, 10, paper("paper", withPadding(key, 10, child)))

  def toggledRow(item: Obj) =
    Toggled(item(filterAttrs.isExpanded))(Some(()=>item(filterAttrs.isExpanded)=true))
  def toggledRow(filterObj: Obj, key: String) =
    Toggled(filterObj(filterAttrs.expandedItem)==key)(Some(()=>filterObj(filterAttrs.expandedItem)=key))


  def paperTable(key: VDomKey)(tableElements: List[TableElement with ChildOfTable]): ChildPair[OfDiv] = {
    val tableWidth = dtTablesState.widthOfTable(key)
    paperWithMargin(key,
      flexGrid("flexGrid",
        flexGridItemWidthSync("widthSync",w⇒tableWidth.value=w.toFloat,
          table("1",Width(tableWidth.value))(tableElements:_*)
        )
      )
    )
  }

  def selectAllCheckBox(itemList: ItemList) = List(
    checkBox("1",
      itemList.filter(filterAttrs.selectedItems).nonEmpty,
      on ⇒
        if(on) itemList.selectAllListed()
        else itemList.filter(filterAttrs.selectedItems)=Set[ObjId]()
    )
  )



  private def entryListView(pf: String) = wrapDBView{ ()=>{
    val filterObj = filters.filterObj("/entryList")
    val itemList = filters.itemList(findEntry,findNodes.justIndexed,filterObj)
    root(List( //class LootsBoatLogList
      toolbar("Entry List"),
      withMaxWidth("1",1200,List(paperTable("dtTableList2")(
        List(
          controlPanel("",btnDelete("1", itemList.removeSelected),btnAdd("2", itemList.add)),
          row("row",MaxVisibleLines(2),IsHeader)(
            group("1_grp",MinWidth(50),MaxWidth(50),Priority(0),TextAlignCenter,Caption("x1")),
            cell("1",MinWidth(50),VerticalAlignMiddle)((_)=>
              selectAllCheckBox(itemList)
            ),
            group("2_grp",MinWidth(150),Priority(3),TextAlignCenter,Caption("x2")),
            cell("2",MinWidth(100),VerticalAlignMiddle)((_)=>
              withSideMargin("1",10,List(text("1","Boat")))::Nil
            ),
            cell("3",MinWidth(150),VerticalAlignMiddle)(_=>
              withSideMargin("1",10,List(text("1","Date")))::Nil
            ),
            cell("4",MinWidth(180),VerticalAlignMiddle)((_)=>
              withSideMargin("1",10,List(text("1","Total duration, hrs:min")))::Nil
            ),
            cell("5",MinWidth(100),VerticalAlignMiddle)((_)=>
              withSideMargin("1",10,List(text("1","Confirmed")))::Nil
            ),
            cell("6",MinWidth(150),VerticalAlignMiddle)((_)=>
              withSideMargin("1",10,List(text("1","Confirmed by")))::Nil
            ),
            cell("7",MinWidth(150),VerticalAlignMiddle)((_)=>
              withSideMargin("1",10,List(text("1","Confirmed on")))::Nil
            ),
            cell("8",MinWidth(100),Priority(0),TextAlignCenter,VerticalAlignMiddle)((_)=>
              withSideMargin("1",10,List(text("1","xx")))::Nil
            )
          )
        ) :::
        itemList.list.map{ (entry:Obj)=>
          val entrySrcId = entry(alien.objIdStr)
          val go = Some(()⇒ currentVDom.relocate(s"/entryEdit/$entrySrcId"))
          row(entrySrcId,
            toggledRow(entry),
            IsSelected(entry(filterAttrs.isSelected)),
            MaxVisibleLines(2))(
            group("1_grp", MinWidth(50),MaxWidth(50), Priority(1),TextAlignCenter, Caption("x1")),
            cell("1", MinWidth(50),VerticalAlignMiddle)((_)=>
              booleanField(entry, filterAttrs.isSelected, editable = true)
            ),
            group("2_grp", MinWidth(150),Priority(3), TextAlignCenter,Caption("x2")),
            cell("2",MinWidth(100))((showLabel)=>
              withSideMargin("1", 10, objField(entry, logAt.boat, editable = false,"Boat",showLabel)) :: Nil
            ),
            cell("3",MinWidth(150),VerticalAlignMiddle)((showLabel)=>
              withSideMargin("1",10,
                dateField(entry, logAt.date, editable = false,"Date",showLabel)
              )::Nil
            ),
            cell("4",MinWidth(180),VerticalAlignMiddle)((showLabel)=>
              withSideMargin("1",10,
                durationField(entry, logAt.durationTotal,"Total duration, hrs:min",showLabel)
              )::Nil
            ),
            cell("5",MinWidth(100),VerticalAlignMiddle)((_)=>
              withSideMargin("1",10,{
                val confirmed = entry(logAt.asConfirmed)
                if(confirmed(nonEmpty))
                  List(materialChip("1","CONFIRMED"))
                else Nil
              })::Nil
            ),
            cell("6",MinWidth(150),VerticalAlignMiddle)((showLabel)=>
              withSideMargin("1",10,objField(entry, logAt.confirmedBy, editable = false,"Confirmed by",showLabel))::Nil
            ),
            cell("7",MinWidth(150),VerticalAlignMiddle)((showLabel)=>
              withSideMargin("1",10,dateField(entry, logAt.confirmedOn, editable = false,"Confirmed on",showLabel))::Nil
            ),
            cell("8",MinWidth(100),Priority(0),TextAlignCenter,VerticalAlignMiddle)((_)=>
              btnCreate("btn2",go.get)::Nil
            )
          )
        }
      )))
    ))
  }}
  // currentVDom.invalidate() ?

  private def entryEditView(pf: String) = wrapDBView { () =>
    //println(pf)
    val obj = findNodes.whereObjId(findNodes.toObjId(UUID.fromString(pf.tail)))
    editViewInner(alien.wrap(obj(logAt.asEntry)))
  }

  var selectDropShow=false
  var selectDropShow1=false
  private def selectDropShowHandle()= selectDropShow = !selectDropShow
  private def selectDropShowHandle1() = selectDropShow1 = !selectDropShow1

  private def editViewInner(entry: Obj): VDomValue = {
    val editable = true /*todo rw rule*/

    val entryIdStr = entry(alien.objIdStr)

    root(List(
      toolbar("Boat Edit"),
      withMaxWidth("1",1200,List(
      paperWithMargin(s"$entryIdStr-1",
        flexGrid("flexGridEdit1",List(
          flexGridItem("1",500,None,List(
            flexGrid("FlexGridEdit11",List(

              flexGridItem("boat",150,None,
                fieldPopupBox("1",selectDropShow,divClickable("1",Some(selectDropShowHandle),labeledText("1","aaa","a2"))::Nil,
                  divNoWrap("1",text("1","aaa"))::
                    divNoWrap("2",text("1","aaa sdfsdfs sds fs df sfs fsfsf sfs dfsfs fdf fs fsfgs f sd"))::
                    (0 to 4).map(x=>{
                    divNoWrap("3"+x,text("1","aaa"))}).toList

                )::Nil //objField(entry,logAt.boat,editable = false,"Boat",showLabel = true)
              ),
              flexGridItem("boat1",100,None,
                fieldPopupBox("1",selectDropShow1,divClickable("1",Some(selectDropShowHandle1),labeledText("1","aaa","a2"))::Nil,
                  divNoWrap("1",text("1","aaa"))::
                    divNoWrap("2",text("1","aaa sdfsdfs sds fs d"))::
                    (0 to 20).map(x=>{
                    divNoWrap("3"+x,text("1","aaa"))}).toList

                )::Nil //objField(entry,logAt.boat,editable = false,"Boat",showLabel = true)
              ),
              flexGridItem("date",150,None,dateField(entry, logAt.date, editable,"Date",showLabel = true)),

              flexGridItem("dur",170,None,List(divAlignWrapper("1","left","middle",
                durationField(entry,logAt.durationTotal,"Total duration, hrs:min",showLabel = true))))
            ))
          )),
          flexGridItem("2",500,None,List(
            flexGrid("flexGridEdit12",List(
              flexGridItem("conf_by",150,None,objField(entry,logAt.confirmedBy,editable = false,"Confirmed by",showLabel = true)),
              flexGridItem("conf_on",150,None,dateField(entry, logAt.confirmedOn, editable = false,"Confirmed on",showLabel = true)),
              flexGridItem("conf_do",150,None,List(
                divHeightWrapper("1",72,
                  divAlignWrapper("1","right","bottom",

                    if(!entry(nonEmpty)) Nil
                    else if(entry(logAt.asConfirmed)(nonEmpty))
                      List(btnRaised("reopen","Reopen")(()⇒entry(logAt.confirmedOn)=None))
                    else
                      List(btnRaised("confirm","Confirm")(()⇒entry(logAt.confirmedOn)=Option(Instant.now())))

                  ))
              ))
            ))
          )))
        )
      ))),

      withMaxWidth("2",1200,List(entryEditFuelScheduleView(entry, editable))),
      withMaxWidth("3",1200,List(entryEditWorkListView(entry, editable)))
    ))
  }

  def entryEditFuelScheduleView(entry: Obj, editable: Boolean): ChildPair[OfDiv] = {
    val filterObj = filters.filterObj(s"/entry/${entry(alien.objIdStr)}")
    paperTable("dtTableEdit1")(List(
      row("row",IsHeader)(
        cell("1",MinWidth(100),Priority(3),VerticalAlignMiddle)((_)=>withSideMargin("1",10,List(text("1","Time")))::Nil),
        cell("2",MinWidth(150),Priority(1),VerticalAlignMiddle)((_)=>withSideMargin("1",10,List(text("1","ME Hours.Min")))::Nil),
        cell("3",MinWidth(100),Priority(1),VerticalAlignMiddle)((_)=>withSideMargin("1",10,List(text("1","Fuel rest/quantity")))::Nil),
        cell("4",MinWidth(250),Priority(3),VerticalAlignMiddle)((_)=>withSideMargin("1",10,List(text("1","Comment")))::Nil),
        cell("5",MinWidth(150),Priority(2),VerticalAlignMiddle)((_)=>withSideMargin("1",10,List(text("1","Engineer")))::Nil),
        cell("6",MinWidth(150),Priority(2),VerticalAlignMiddle)((_)=>withSideMargin("1",10,List(text("1","Master")))::Nil)
      ),
      row("row1",toggledRow(filterObj,"row1"))(
        cell("1",MinWidth(100),Priority(3),VerticalAlignMiddle)((showLabel)=>
          withSideMargin("1",10,List(if(showLabel) labeledText("1","00:00","Time") else text("1","00:00")))::Nil
        ),
        cell("2",MinWidth(150),Priority(1),VerticalAlignMiddle)((showLabel)=>
          withSideMargin("1",10, timeField(entry, logAt.log00Date, editable, "Date", showLabel))::Nil
        ),
        cell("3",MinWidth(100),Priority(1),VerticalAlignMiddle)((showLabel)=>
          withSideMargin("1",10, strField(entry, logAt.log00Fuel, editable,"Fuel rest/quantity",showLabel))::Nil
        ),
        cell("4",MinWidth(250),Priority(3),VerticalAlignMiddle)((showLabel)=>
          withSideMargin("1",10, strField(entry, logAt.log00Comment, editable,"Comment",showLabel))::Nil
        ),
        cell("5",MinWidth(150),Priority(2),VerticalAlignMiddle)((showLabel)=>
          withSideMargin("1",10, strField(entry, logAt.log00Engineer, editable,"Engineer",showLabel))::Nil
        ),
        cell("6",MinWidth(150),Priority(2),VerticalAlignMiddle)((showLabel)=>
          withSideMargin("1",10, strField(entry, logAt.log00Master, editable,"Master",showLabel))::Nil
        )
      ),
      row("row2",toggledRow(filterObj,"row2"))(
        cell("1",MinWidth(100),Priority(3),VerticalAlignMiddle)((showLabel)=>
          withSideMargin("1",10,List(if(showLabel) labeledText("1","08:00","Time") else text("1","08:00")))::Nil
        ),
        cell("2",MinWidth(150),Priority(1),VerticalAlignMiddle)((showLabel)=>
          withSideMargin("1",10, timeField(entry, logAt.log08Date, editable, "Date", showLabel))::Nil
        ),
        cell("3",MinWidth(100),Priority(1),VerticalAlignMiddle)((showLabel)=>
          withSideMargin("1",10, strField(entry, logAt.log08Fuel, editable,"Fuel rest/quantity",showLabel))::Nil
        ),
        cell("4",MinWidth(250),Priority(3),VerticalAlignMiddle)((showLabel)=>
          withSideMargin("1",10, strField(entry, logAt.log08Comment, editable,"Comment",showLabel))::Nil
        ),
        cell("5",MinWidth(150),Priority(2),VerticalAlignMiddle)((showLabel)=>
          withSideMargin("1",10, strField(entry, logAt.log08Engineer, editable,"Engineer",showLabel))::Nil
        ),
        cell("6",MinWidth(150),Priority(2),VerticalAlignMiddle)((showLabel)=>
          withSideMargin("1",10, strField(entry, logAt.log08Master, editable,"Master",showLabel))::Nil
        )

      ),
      row("row3",toggledRow(filterObj,"row3"))(
        cell("1",MinWidth(100),Priority(3),VerticalAlignMiddle)((_)=>withSideMargin("1",10,List(text("1","Passed")))::Nil),
        cell("2",MinWidth(150),Priority(1),VerticalAlignMiddle)((_)=>withSideMargin("1",10,List(text("1","Received Fuel")))::Nil),
        cell("3",MinWidth(100),Priority(1),VerticalAlignMiddle)((showLabel)=>
          withSideMargin("1",10,strField(entry, logAt.logRFFuel, editable,"Fuel rest/quantity",showLabel))::Nil
        ),
        cell("4",MinWidth(250),Priority(3),VerticalAlignMiddle)((showLabel)=>
          withSideMargin("1",10,strField(entry, logAt.logRFComment, editable,"Comment",showLabel))::Nil),
        cell("5",MinWidth(150),Priority(2),VerticalAlignMiddle)((showLabel)=>
          withSideMargin("1",10,strField(entry, logAt.logRFEngineer, editable,"Engineer",showLabel))::Nil),
        cell("6",MinWidth(150),Priority(2),VerticalAlignMiddle)((_)=>withSideMargin("1",10,Nil)::Nil)

      ),
      row("row4", toggledRow(filterObj,"row4"))(
        cell("1",MinWidth(100),Priority(3),VerticalAlignMiddle)((showLabel)=>
          withSideMargin("1",10, List(if(showLabel) labeledText("1","24:00","Time") else text("1","24:00")))::Nil
        ),
        cell("2",MinWidth(150),Priority(1),VerticalAlignMiddle)((showLabel)=>
          withSideMargin("1",10, timeField(entry, logAt.log24Date, editable, "Date", showLabel))::Nil
        ),
        cell("3",MinWidth(100),Priority(1),VerticalAlignMiddle)((showLabel)=>
          withSideMargin("1",10, strField(entry, logAt.log24Fuel, editable,"Fuel rest/quantity",showLabel))::Nil
        ),
        cell("4",MinWidth(250),Priority(3),VerticalAlignMiddle)((showLabel)=>
          withSideMargin("1",10, strField(entry, logAt.log24Comment, editable,"Comment",showLabel))::Nil
        ),
        cell("5",MinWidth(150),Priority(2),VerticalAlignMiddle)((showLabel)=>
          withSideMargin("1",10, strField(entry, logAt.log24Engineer, editable,"Engineer",showLabel))::Nil
        ),
        cell("6",MinWidth(150),Priority(2),VerticalAlignMiddle)((showLabel)=>
          withSideMargin("1",10, strField(entry, logAt.log24Master, editable,"Master",showLabel))::Nil
        )
      )
    ))
  }

  def entryEditWorkListView(entry: Obj, editable: Boolean): ChildPair[OfDiv] = {
    val entryIdStr = entry(alien.objIdStr)
    val filterObj = filters.filterObj(s"/entryEditWorkList/$entryIdStr")
    val workList = filters.itemList(findWorkByEntry,entry,filterObj)
    paperTable("dtTableEdit2")(
      List(
        controlPanel("",btnDelete("1", workList.removeSelected),btnAdd("2", workList.add)),
        row("row",IsHeader)(
          group("1_group",MinWidth(50),MaxWidth(50),Priority(0)),
          cell("1",MinWidth(50),VerticalAlignMiddle)((_)=>selectAllCheckBox(workList)),
          group("2_group",MinWidth(150)),
          cell("2",MinWidth(100),VerticalAlignMiddle)((_)=>withSideMargin("1",10,List(text("1","Start")))::Nil),
          cell("3",MinWidth(100),VerticalAlignMiddle)((_)=>withSideMargin("1",10,List(text("1","Stop")))::Nil),
          cell("4",MinWidth(150),VerticalAlignMiddle)((_)=>withSideMargin("1",10,List(text("1","Duration, hrs:min")))::Nil),
          cell("5",MinWidth(250),Priority(3),VerticalAlignMiddle)((_)=>withSideMargin("1",10,List(text("1","Comment")))::Nil)
        )
      ) :::
      workList.list.map { (work: Obj) =>
        val workSrcId = work(alien.objIdStr)
        row(workSrcId,
          toggledRow(work),
          IsSelected(work(filterAttrs.isSelected))
        )(
          group("1_group",MinWidth(50),MaxWidth(50),Priority(0)),
          cell("1",MinWidth(50),VerticalAlignMiddle)(_=>
            booleanField(work, filterAttrs.isSelected, editable)
          ),
          group("2_group",MinWidth(150)),
          cell("2",MinWidth(100),VerticalAlignMiddle)((showLabel)=>
            withSideMargin("1",10,timeField(work, logAt.workStart, editable, "Start", showLabel))::Nil
          ),
          cell("3",MinWidth(100),VerticalAlignMiddle)((showLabel)=>
            withSideMargin("1",10,timeField(work, logAt.workStop, editable, "Stop", showLabel))::Nil
          ),
          cell("4",MinWidth(150),VerticalAlignMiddle)((showLabel)=>
            withSideMargin("1",10,durationField(work, logAt.workDuration,"Duration, hrs:min",showLabel))::Nil
          ),
          cell("5",MinWidth(250),Priority(3),VerticalAlignMiddle)((showLabel)=>
            withSideMargin("1",10,strField(entry, logAt.workComment, editable,"Comment",showLabel))::Nil
          )
        )
      }
    )

  }


  //// users
  private def loginView() = {
    val editable = true
    val showLabel = true
    val dialog = filters.filterObj("/login")
    root(List(paperTable("login")(List(row("1", Nil)(List(
      cell("1",MinWidth(250))(_⇒strField(dialog, userAttrs.username, editable, "Username", showLabel)),
      cell("2",MinWidth(250))(_⇒strField(dialog, userAttrs.unEncryptedPassword, editable, "Password", showLabel)),
      cell("3",MinWidth(250))(_⇒
        users.loginAction(dialog).map(
          btnRaised("doChange","Change Password")(_)
        ).toList
      )
    ))))))
  }

  private def userListView(pf: String) = wrapDBView { () =>
    val filterObj = filters.filterObj("/userList")
    val userList = filters.itemList(users.findAll, findNodes.justIndexed, filterObj)
    val editable = true //todo
    root(List(
      toolbar("Users"),
      btnAdd("2", userList.add),
      paperTable("table")(
        controlPanel("",btnDelete("1", userList.removeSelected),btnAdd("2", userList.add)) ::
        row("head",IsHeader)(
          cell("0",MinWidth(250))(_⇒selectAllCheckBox(userList)),
          cell("1",MinWidth(250))(_⇒List(text("text", "Full Name"))),
          cell("2",MinWidth(250))(_⇒Nil)
        ) ::
        userList.list.map{ obj ⇒
          val user = alien.wrap(obj)
          val srcId = user(alien.objIdStr)
          row(srcId)(
            cell("0",MinWidth(250))(_⇒booleanField(user,filterAttrs.isSelected, editable = true)),
            cell("1",MinWidth(250))(showLabel⇒strField(user, at.caption, editable = true, "User", showLabel)),
            cell("2",MinWidth(250))(showLabel⇒
              if(showLabel)
                strField(user, userAttrs.unEncryptedPassword, editable, "New Password", showLabel) :::
                strField(user, userAttrs.unEncryptedPasswordAgain, editable, "Repeat Password", showLabel) :::
                List(btnRaised("doChange","Change Password")(users.changePasswordAction(user)))
              else Nil
            )
          )
        }
      )
    ))
  }

  //// events
  private def eventListView(pf: String) = wrapDBView { () =>
    root(List(
      toolbar("Events"),
      paperTable("table")(
        row("head", IsHeader)(
          cell("1",MinWidth(250))(_⇒List(text("text", "Event"))),
          cell("2",MinWidth(250))(_⇒Nil)
        ) ::
        eventSource.unmergedEvents.map(alien.wrap).map { ev =>
          val srcId = ev(alien.objIdStr)
          row(srcId)(
            cell("1",MinWidth(250))(_⇒List(text("text", ev(eventSource.comment)))),
            cell("2",MinWidth(250))(_⇒List(btnRemove("btn", () => eventSource.addUndo(ev))))
          )
        }
      )
    ))
  }
  private def eventToolbarButtons() = if (eventSource.unmergedEvents.isEmpty) Nil
    else List(
      btnRestore("events", () ⇒ currentVDom.relocate("/eventList")),
      btnSave("save", ()⇒eventSource.addRequest())
    )
  private def eventListHandlers = CoHandler(ViewPath("/eventList"))(eventListView) :: Nil

  //// calculations
  private def calcWorkDuration(on: Boolean, work: Obj): Unit = {
    work(logAt.workDuration) = if(!on) None else
      Option(Duration.between(work(logAt.workStart).get, work(logAt.workStop).get))
  }
  private def calcEntryDuration(on: Boolean, work: Obj): Unit = {
    val entry = work(logAt.entryOfWork)
    val was = entry(logAt.durationTotal).getOrElse(Duration.ofSeconds(0L))
    val delta = work(logAt.workDuration).get
    entry(logAt.durationTotal) =
      Option(if(on) was.plus(delta) else was.minus(delta))
  }
  private def calcConfirmed(on: Boolean, entry: Obj): Unit =
    entry(logAt.asConfirmed) = if(on) entry else findNodes.noNode
  private def calcHandlers() =
    onUpdate.handlers(List(logAt.asWork,logAt.workStart,logAt.workStop).map(attrFactory.attrId(_)), calcWorkDuration) :::
      onUpdate.handlers(List(logAt.asWork,logAt.workDuration,logAt.entryOfWork).map(attrFactory.attrId(_)), calcEntryDuration) :::
      onUpdate.handlers(List(logAt.asEntry,logAt.confirmedOn).map(attrFactory.attrId(_)), calcConfirmed)

  ////

  private def toolbar(title:String): ChildPair[OfDiv] = {
    paperWithMargin("toolbar", divWrapper("toolbar",None,Some("200px"),None,None,None,None,
      divWrapper("1",Some("inline-block"),None,None,Some("50px"),None,None,
        divAlignWrapper("1","left","middle",text("title",title)::Nil)::Nil
      )::
      divWrapper("2",None,None,None,None,Some("right"),None,
        btnRaised("users","Users")(()⇒currentVDom.relocate("/userList")) ::
        btnViewList("entries",()=>currentVDom.relocate("/entryList")) ::
        eventToolbarButtons()
      )::Nil
    ))
  }

  def handlers =
    List(findEntry,findWorkByEntry).flatMap(searchIndex.handlers(_)) :::
    List(
      logAt.durationTotal, logAt.asConfirmed, logAt.confirmedBy, logAt.workDuration
    ).flatMap(factIndex.handlers(_)) :::
    List(
      at.caption,
      logAt.asEntry, logAt.asWork, logAt.asBoat, // <-create
      logAt.boat, logAt.confirmedOn, logAt.entryOfWork,
      logAt.date, logAt.workStart, logAt.workStop, logAt.workComment,
      logAt.log00Date,logAt.log00Fuel,logAt.log00Comment,logAt.log00Engineer,logAt.log00Master,
      logAt.log08Date,logAt.log08Fuel,logAt.log08Comment,logAt.log08Engineer,logAt.log08Master,
      logAt.logRFFuel,logAt.logRFComment,logAt.logRFEngineer,
      logAt.log24Date,logAt.log24Fuel,logAt.log24Comment,logAt.log24Engineer,logAt.log24Master
    ).flatMap{ attr⇒
      factIndex.handlers(attr) ::: alien.update(attr)
    } :::
    alien.update(findAttrs.justIndexed) :::
    CoHandler(ViewPath(""))(emptyView) ::
    CoHandler(ViewPath("/userList"))(userListView) ::
    //CoHandler(ViewPath("/boatList"))(boatListView) ::
    CoHandler(ViewPath("/entryList"))(entryListView) ::
    CoHandler(ViewPath("/entryEdit"))(entryEditView) ::
    eventListHandlers :::
    calcHandlers :::
    CoHandler(SessionInstantAdded)(currentVDom.invalidate) ::
    CoHandler(TransientChanged)(currentVDom.invalidate) ::
    Nil
}


class UserAttrs(
  attr: AttrFactory,
  label: LabelFactory,
  asDBObj: AttrValueType[Obj],
  asString: AttrValueType[String],
  asUUID: AttrValueType[Option[UUID]]
)(
  val asUser: Attr[Obj] = label("f8c8d6da-0942-40aa-9005-261e63498973"),
  val username: Attr[String] = attr("4f0d01f8-a1a3-4551-9d07-4324d4d0e633",asString),
  val encryptedPassword: Attr[Option[UUID]] = attr("3a345f93-18ab-4137-bdde-f0df77161b5f",asUUID),
  val unEncryptedPassword: Attr[String] = attr("7d12edd9-a162-4305-8a0c-31ef3f2e3300",asString),
  val unEncryptedPasswordAgain: Attr[String] = attr("24517821-c606-4f6c-8e93-4f01c2490747",asString),
  val authenticatedUser: Attr[Obj] = attr("47ee2460-b170-4213-9d56-a8fe0f7bc1f5",asDBObj)
)

class Users(
  at: UserAttrs, nodeAttrs: NodeAttrs, findAttrs: FindAttrs,
  handlerLists: CoHandlerLists,
  factIndex: FactIndex, searchIndex: SearchIndex,
  findNodes: FindNodes, mainTx: CurrentTx[MainEnvKey],
  alien: Alien, transient: Transient
)(
  val findAll: SearchByLabelProp[String] = searchIndex.create(at.asUser, findAttrs.justIndexed),
  val findByName: SearchByLabelProp[String] = searchIndex.create(at.asUser, at.username)
) extends CoHandlerProvider {
  private def eventSource = handlerLists.single(SessionEventSource, ()⇒Never())
  private def encryptPassword(user: Obj, pw: String): UUID = {
    val buffer = ByteBuffer.allocate(256)
    val objId = user(nodeAttrs.objId)
    val username = user(at.username)
    buffer.putLong(objId.hi).putLong(objId.lo).put(Bytes(username)).put(Bytes(pw))
    UUID.nameUUIDFromBytes(buffer.array())
  }
  def changePasswordAction(user: Obj)(): Unit = {
    val pw = user(at.unEncryptedPassword)
    if(pw != user(at.unEncryptedPasswordAgain))
      throw new Exception("Passwords do not match")
    user(at.encryptedPassword) = Some(encryptPassword(user,pw))
  }
  def loginAction(dialog: Obj): Option[()⇒Unit] = {
    val username = dialog(at.username)
    if(username.isEmpty){ return None }
    val user = findNodes.single(findNodes.where(mainTx(), findByName, dialog(at.username), Nil))
    val mainSession = eventSource.mainSession
    Some {
      () ⇒
        val pw = dialog(at.unEncryptedPassword)
        if(user(findAttrs.nonEmpty) &&
          encryptPassword(user, pw) == user(at.encryptedPassword).get)
          mainSession(at.authenticatedUser) = user
        else throw new Exception("Bad username or password")
    }
  }
  def handlers =
    List(findAll,findByName).flatMap(searchIndex.handlers) :::
    List(at.unEncryptedPassword, at.unEncryptedPasswordAgain).flatMap(transient.update) :::
    List(at.asUser,at.username,at.encryptedPassword,at.authenticatedUser).flatMap{ attr⇒
      factIndex.handlers(attr) ::: alien.update(attr)
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



