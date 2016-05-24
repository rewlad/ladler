package ee.cone.base.test_loots

import java.time.format.DateTimeFormatter
import java.time._
import java.util.UUID

import ee.cone.base.connection_api._
import ee.cone.base.db._
import ee.cone.base.server.SenderOfConnection
import ee.cone.base.util.Never
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

  val asUser: Attr[Obj] = label("f8c8d6da-0942-40aa-9005-261e63498973"),
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
  private val rowToggledOfTables = collection.mutable.Map[VDomKey,VDomKey]()
  def toggledRow(tableId:VDomKey, rowId:VDomKey) =
    Toggled(rowToggledOfTables.get(tableId).exists(_==rowId))(Some{()=>
      rowToggledOfTables(tableId) = rowId
      currentVDom.invalidate()
    })
}



///////

class FilterAttrs(
  attr: AttrFactory,
  label: LabelFactory,
  asString: AttrValueType[String],
  asBoolean: AttrValueType[Boolean],
  asObjIdSet: AttrValueType[Set[ObjId]]
)(
  val asFilter: Attr[Obj] = label("eee2d171-b5f2-4f8f-a6d9-e9f3362ff9ed"),
  val filterFullKey: Attr[String] = attr("2879097b-1fd6-45b1-a8b4-1de807ce9572",asString),
  val isSelected: Attr[Boolean] = attr("a5045617-279f-48b8-95a9-a42dc721d67b",asBoolean),
  val isListed: Attr[Boolean] = attr("bd68ccbc-b63c-45ce-88f2-7c6058b11338",asBoolean),
  val selectedItems: Attr[Set[ObjId]] = attr("32a62c43-e837-4855-985a-d79f5dc03db0",asObjIdSet)
)

trait InnerItemList {
  def isSelected(obj: Obj): Boolean
  def setSelected(obj: Obj): Unit
  def resetSelected(obj: Obj): Unit
  def isListed(obj: Obj): Boolean
  def setListed(obj: Obj): Unit
  def resetListed(obj: Obj): Unit
}

trait ItemList {
  def filter: Obj
  def add(): Unit
  def list: List[Obj]
  def selectAllListed(): Unit
  def removeSelected(): Unit
}

class ObjIdSetValueConverter(
  val valueType: AttrValueType[Set[ObjId]], inner: RawConverter, findNodes: FindNodes
) extends RawValueConverterImpl[Set[ObjId]] {
  private def splitter = " "
  def convertEmpty() = Set()
  def convert(valueA: Long, valueB: Long) = Never()
  def convert(value: String) =
    value.split(splitter).map(s⇒findNodes.toObjId(UUID.fromString(s))).toSet
  def toBytes(preId: ObjId, value: Value, finId: ObjId) =
    if(value.nonEmpty) inner.toBytes(preId, value.toSeq.map(findNodes.toUUIDString).sorted.mkString(splitter), finId) else Array()
}


class Filters(
  at: FilterAttrs,
  nodeAttrs: NodeAttrs,
  handlerLists: CoHandlerLists,
  attrFactory: AttrFactory,
  findNodes: FindNodes,
  mainTx: CurrentTx[MainEnvKey],
  alien: Alien,
  listedWrapType: WrapType[InnerItemList],
  factIndex: FactIndex,
  searchIndex: SearchIndex
)(
  val filterByFullKey: SearchByLabelProp[String] = searchIndex.create(at.asFilter,at.filterFullKey)
) extends CoHandlerProvider {
  private def eventSource = handlerLists.single(SessionEventSource, ()⇒Never())
  private def lazyLinkingObj[Value](index: SearchByLabelProp[Value], key: Value): Obj =
    findNodes.where(mainTx(), index, key, Nil) match {
      case Nil ⇒ alien.demandedNode { obj ⇒
        obj(attrFactory.toAttr(index.labelId, index.labelType)) = obj
        obj(attrFactory.toAttr(index.propId, index.propType)) = key
      }
      case o :: Nil ⇒ alien.wrap(o)
      case _ ⇒ Never()
    }
  def filterObj(key: String): Obj =
    lazyLinkingObj(filterByFullKey, s"${eventSource.sessionKey}$key")
  def itemList[Value](
    index: SearchByLabelProp[Value],
    parentValue: Value,
    filterObj: Obj
  ): ItemList = {
    val selectedSet = filterObj(at.selectedItems)
    val parentAttr = attrFactory.toAttr(index.propId, index.propType)
    val asType = attrFactory.toAttr(index.labelId, index.labelType)

    val inner = new InnerItemList {
      def isSelected(obj: Obj) = selectedSet contains obj(nodeAttrs.objId)
      def setSelected(obj: Obj) =
        filterObj(at.selectedItems) = selectedSet + obj(nodeAttrs.objId)
      def resetSelected(obj: Obj) =
        filterObj(at.selectedItems) = selectedSet - obj(nodeAttrs.objId)
      def isListed(obj: Obj) = obj(parentAttr) == parentValue
      def setListed(obj: Obj) = obj(parentAttr) = parentValue
      def resetListed(obj: Obj) = obj(factIndex.defined(attrFactory.attrId(parentAttr))) = false
    }
    val items = findNodes.where(mainTx(), index, parentValue, Nil)
      .map(obj⇒
        alien.wrap(obj).wrap(listedWrapType,inner)
      )
    val newItem = alien.demandedNode{ obj ⇒ obj(asType) = obj }.wrap(listedWrapType,inner)
    new ItemList {
      def filter = filterObj
      def list = items
      def add() = newItem(at.isListed) = true
      def removeSelected() = {
        selectedSet.foreach(findNodes.whereObjId(_)(at.isListed)=false)
        filter(at.selectedItems) = Set[ObjId]()
      }
      def selectAllListed() =
        filter(at.selectedItems) = selectedSet ++ items.map(_(nodeAttrs.objId))
    }
  }


  def handlers = List(
    CoHandler(GetValue(listedWrapType,at.isSelected)){ (obj,innerObj)⇒
      innerObj.data.isSelected(obj)
    },
    CoHandler(SetValue(listedWrapType,at.isSelected)){ (obj,innerObj,value)⇒
      if(value) innerObj.data.setSelected(obj) else innerObj.data.resetSelected(obj)
    },
    CoHandler(GetValue(listedWrapType,at.isListed)){ (obj,innerObj)⇒
      innerObj.data.isListed(obj)
    },
    CoHandler(SetValue(listedWrapType,at.isListed)){ (obj,innerObj,value)⇒
      if(value) innerObj.data.setListed(obj) else innerObj.data.resetListed(obj)
    }
  ) ::: List(at.asFilter,at.filterFullKey,at.selectedItems).flatMap{ attr⇒
    factIndex.handlers(attr) ::: alien.update(attr)
  } :::
  searchIndex.handlers(filterByFullKey)
}

class TestComponent(
  nodeAttrs: NodeAttrs, findAttrs: FindAttrs,
  filterAttrs: FilterAttrs, at: TestAttributes, logAt: BoatLogEntryAttributes,
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
  htmlTableWithControl: HtmlTableWithControl
)(
  val findUser: SearchByLabelProp[String] = searchIndex.create(logAt.asUser, findAttrs.justIndexed),
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
      List(labeledText("1",label,valueToText(obj(attr))))

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
      labeledText("1", visibleLabel, dateStr) :: Nil
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

  private def wrapDBView[R](view: ()=>R): R =
    eventSource.incrementalApplyAndView { () ⇒
      if(!loggedIn){ /*return loginView() */???}
      val startTime = System.currentTimeMillis
      val res = view()
      val endTime = System.currentTimeMillis
      currentVDom.until(endTime+(endTime-startTime)*10)
      res
    }

  private def paperWithMargin(key: VDomKey, child: ChildPair[OfDiv]) =
    withMargin(key, 10, paper("paper", withPadding(key, 10, child)))


  private def mCell(key:VDomKey,minWidth: Int)(handle:(Boolean)=>List[ChildPair[OfDiv]])=
    cell(key,MinWidth(minWidth),VerticalAlignMiddle)(showLabel=>withSideMargin("1",10,handle(showLabel))::Nil)
  private def mCell(key:VDomKey,minWidth: Int,priority: Int)(handle:(Boolean)=>List[ChildPair[OfDiv]])=
    cell(key,MinWidth(minWidth),Priority(priority),VerticalAlignMiddle)(showLabel=>withSideMargin("1",10,handle(showLabel))::Nil)
  private def mcCell(key:VDomKey,minWidth: Int)(handle:(Boolean)=>List[ChildPair[OfDiv]])=
    cell(key,MinWidth(minWidth),TextAlignCenter,VerticalAlignMiddle)(showLabel=>withSideMargin("1",10,handle(showLabel))::Nil)
  private def mcCell(key:VDomKey,minWidth: Int, priority:Int)(handle:(Boolean)=>List[ChildPair[OfDiv]])=
    cell(key,MinWidth(minWidth),Priority(priority),TextAlignCenter,VerticalAlignMiddle)(showLabel=>withSideMargin("1",10,handle(showLabel))::Nil)

  def loggedIn = true //todo
  private def loginView() = {

  }


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
            mCell("1",50)((_)=> selectAllCheckBox(itemList)),
            group("2_grp",MinWidth(150),Priority(3),TextAlignCenter,Caption("x2")),
            mCell("2",100)((_)=>List(text("1","Boat"))),
            mCell("3",150)(_=>List(text("1","Date"))),
            mCell("4",180)((_)=>List(text("1","Total duration, hrs:min"))),
            mCell("5",100)((_)=>List(text("1","Confirmed"))),
            mCell("6",150)((_)=>List(text("1","Confirmed by"))),
            mCell("7",150)((_)=>List(text("1","Confirmed on"))),
            mcCell("8",100,0)((_)=>List(text("1","xx")))
          )
        ) :::
        itemList.list.map{ (entry:Obj)=>
          val entrySrcId = entry(alien.objIdStr)
          val go = Some(()⇒ currentVDom.relocate(s"/entryEdit/$entrySrcId"))
          row(entrySrcId,
            dtTablesState.toggledRow("dtTableList2",entrySrcId),
            IsSelected(entry(filterAttrs.isSelected)),
            MaxVisibleLines(2))(
            group("1_grp", MinWidth(50),MaxWidth(50), Priority(1),TextAlignCenter, Caption("x1")),
            mCell("1", 50)((_)=>booleanField(entry, filterAttrs.isSelected, editable = true)),
            group("2_grp", MinWidth(150),Priority(3), TextAlignCenter,Caption("x2")),
            mCell("2",100)((showLabel)=>objField(entry, logAt.boat, editable = false,"Boat",showLabel)),
            mCell("3",150)((showLabel)=>dateField(entry, logAt.date, editable = false,"Date",showLabel)),
            mCell("4",180)((showLabel)=>durationField(entry, logAt.durationTotal,"Total duration, hrs:min",showLabel)),
            mCell("5",100)((_)=>
             {
                val confirmed = entry(logAt.asConfirmed)
                if(confirmed(nonEmpty))
                  List(materialChip("1","CONFIRMED"))
                else Nil
             }
            ),
            mCell("6",150)((showLabel)=>objField(entry, logAt.confirmedBy, editable = false,"Confirmed by",showLabel)),
            mCell("7",150)((showLabel)=>dateField(entry, logAt.confirmedOn, editable = false,"Confirmed on",showLabel)),
            mcCell("8",100,0)((_)=>btnCreate("btn2",go.get)::Nil
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


  def entryEditFuelScheduleView(entry: Obj, editable: Boolean): ChildPair[OfDiv] = {
    paperTable("dtTableEdit1")(List(
      row("row",IsHeader)(
        mCell("1",100,3)((_)=>List(text("1","Time"))),
        mCell("2",150,1)((_)=>List(text("1","ME Hours.Min"))),
        mCell("3",100,1)((_)=>List(text("1","Fuel rest/quantity"))),
        mCell("4",250,3)((_)=>List(text("1","Comment"))),
        mCell("5",150,2)((_)=>List(text("1","Engineer"))),
        mCell("6",150,2)((_)=>List(text("1","Master")))
      ),
      row("row1",dtTablesState.toggledRow("dtTableEdit1","row1"))(
        mCell("1",100,3)((showLabel)=>
          List(if(showLabel) labeledText("1","00:00","Time") else text("1","00:00"))
        ),
        mCell("2",150,1)((showLabel)=>
          timeField(entry, logAt.log00Date, editable, "Date", showLabel)
        ),
        mCell("3",100,1)((showLabel)=>
          strField(entry, logAt.log00Fuel, editable,"Fuel rest/quantity",showLabel)
        ),
        mCell("4",250,3)((showLabel)=>
          strField(entry, logAt.log00Comment, editable,"Comment",showLabel)
        ),
        mCell("5",150,2)((showLabel)=>
          strField(entry, logAt.log00Engineer, editable,"Engineer",showLabel)
        ),
        mCell("6",150,2)((showLabel)=>
          strField(entry, logAt.log00Master, editable,"Master",showLabel)
        )
      ),
      row("row2",dtTablesState.toggledRow("dtTableEdit1","row2"))(
        mCell("1",100,3)((showLabel)=>
          List(if(showLabel) labeledText("1","08:00","Time") else text("1","08:00"))
        ),
        mCell("2",150,1)((showLabel)=>
          timeField(entry, logAt.log08Date, editable, "Date", showLabel)
        ),
        mCell("3",100,1)((showLabel)=>
          strField(entry, logAt.log08Fuel, editable,"Fuel rest/quantity",showLabel)
        ),
        mCell("4",250,3)((showLabel)=>
          strField(entry, logAt.log08Comment, editable,"Comment",showLabel)
        ),
        mCell("5",150,2)((showLabel)=>
          strField(entry, logAt.log08Engineer, editable,"Engineer",showLabel)
        ),
        mCell("6",150,2)((showLabel)=>
          strField(entry, logAt.log08Master, editable,"Master",showLabel)
        )

      ),
      row("row3",dtTablesState.toggledRow("dtTableEdit1","row3"))(
        mCell("1",100,3)((_)=>List(text("1","Passed"))),
        mCell("2",150,1)((_)=>List(text("1","Received Fuel"))),
        mCell("3",100,1)((showLabel)=>
          strField(entry, logAt.logRFFuel, editable,"Fuel rest/quantity",showLabel)
        ),
        mCell("4",250,3)((showLabel)=>
          strField(entry, logAt.logRFComment, editable,"Comment",showLabel)),
        mCell("5",150,2)((showLabel)=>
         strField(entry, logAt.logRFEngineer, editable,"Engineer",showLabel)),
        mCell("6",150,2)((_)=>Nil)

      ),
      row("row4", dtTablesState.toggledRow("dtTableEdit1","row4"))(
        mCell("1",100,3)((showLabel)=>
          List(if(showLabel) labeledText("1","24:00","Time") else text("1","24:00"))
        ),
        mCell("2",150,1)((showLabel)=>
          timeField(entry, logAt.log24Date, editable, "Date", showLabel)
        ),
        mCell("3",100,1)((showLabel)=>
          strField(entry, logAt.log24Fuel, editable,"Fuel rest/quantity",showLabel)
        ),
        mCell("4",250,3)((showLabel)=>
          strField(entry, logAt.log24Comment, editable,"Comment",showLabel)
        ),
        mCell("5",150,2)((showLabel)=>
          strField(entry, logAt.log24Engineer, editable,"Engineer",showLabel)
        ),
        mCell("6",150,2)((showLabel)=>
          strField(entry, logAt.log24Master, editable,"Master",showLabel)
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
          mCell("1",50)((_)=>selectAllCheckBox(workList)),
          group("2_group",MinWidth(150)),
          mCell("2",100)((_)=>List(text("1","Start"))),
          mCell("3",100)((_)=>List(text("1","Stop"))),
          mCell("4",150)((_)=>List(text("1","Duration, hrs:min"))),
          mCell("5",250,3)((_)=>List(text("1","Comment")))
        )
      ) :::
      workList.list.map { (work: Obj) =>
        val workSrcId = work(alien.objIdStr)
        row(workSrcId,
          dtTablesState.toggledRow("dtTableEdit2",workSrcId),
          IsSelected(work(filterAttrs.isSelected))
        )(
          group("1_group",MinWidth(50),MaxWidth(50),Priority(0)),
          mCell("1",50)(_=>
            booleanField(work, filterAttrs.isSelected, editable)
          ),
          group("2_group",MinWidth(150)),
          mCell("2",100)((showLabel)=>
            timeField(work, logAt.workStart, editable, "Start", showLabel)
          ),
          mCell("3",100)((showLabel)=>
            timeField(work, logAt.workStop, editable, "Stop", showLabel)
          ),
          mCell("4",150)((showLabel)=>
            durationField(work, logAt.workDuration,"Duration, hrs:min",showLabel)
          ),
          mCell("5",250,3)((showLabel)=>
            strField(entry, logAt.workComment, editable,"Comment",showLabel)
          )
        )
      }
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

  private def userListView(pf: String) = wrapDBView { () =>
    val filterObj = filters.filterObj("/userList")
    val userList = filters.itemList(findUser, findNodes.justIndexed, filterObj)


    root(List(
      toolbar("Users"),
      btnAdd("2", userList.add),
      paperTable("table")(
        row("head",IsHeader)(
          mCell("0",250)(_⇒selectAllCheckBox(userList)),
          mCell("1",250)(_⇒List(text("text", "Full Name")))
        ) ::
        userList.list.map{ obj ⇒
          val user = alien.wrap(obj)
          val srcId = user(alien.objIdStr)
          row(srcId)(
            mCell("0",250)(_⇒booleanField(user,filterAttrs.isSelected, editable = true)),
            mCell("1",250)(showLabel⇒strField(user, at.caption, editable = true, "User", showLabel))
          )
        }
      )
    ))
  }

  private def eventListView(pf: String) = wrapDBView { () =>
    root(List(
      toolbar("Events"),
      paperTable("table")(
        row("head", IsHeader)(
          mCell("1",250)(_⇒List(text("text", "Event"))),
          mCell("2",250)(_⇒Nil)
        ) ::
        eventSource.unmergedEvents.map(alien.wrap).map { ev =>
          val srcId = ev(alien.objIdStr)
          row(srcId)(
            mCell("1",250)(_⇒List(text("text", ev(eventSource.comment)))),
            mCell("2",250)(_⇒List(btnRemove("btn", () => eventSource.addUndo(ev))))
          )
        }
      )
    ))
  }

  private def saveAction()() = eventSource.addRequest()

  private def toolbar(title:String): ChildPair[OfDiv] = {
    paperWithMargin("toolbar", divWrapper("toolbar",None,Some("200px"),None,None,None,None,
      divWrapper("1",Some("inline-block"),None,None,Some("50px"),None,None,
        divAlignWrapper("1","left","middle",text("title",title)::Nil)::Nil
      )::
      divWrapper("2",None,None,None,None,Some("right"),None,
        iconMenu("menu",
          menuItem("users","Users")(()⇒currentVDom.relocate("/userList")),
          menuItem("entries","Entries")(()=>currentVDom.relocate("/entryList"))
        )::
        {
          if (eventSource.unmergedEvents.isEmpty) Nil
          else List(
            btnRestore("events", () ⇒ currentVDom.relocate("/eventList")),
            btnSave("save", saveAction())
          )
        }
      )::Nil
    ))
  }

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
  private def setEntryConfirmDate(on: Boolean, entry: Obj): Unit =
    entry(logAt.asConfirmed) = if(on) entry else findNodes.noNode


  def handlers =
    List(findUser,findEntry,findWorkByEntry).flatMap(searchIndex.handlers(_)) :::
    List(
      logAt.durationTotal, logAt.asConfirmed, logAt.confirmedBy, logAt.workDuration
    ).flatMap(factIndex.handlers(_)) :::
    List(
      at.caption,
      logAt.asEntry, logAt.asWork, logAt.asUser, logAt.asBoat, // <-create
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
    CoHandler(ViewPath("/eventList"))(eventListView) ::
    CoHandler(ViewPath("/userList"))(userListView) ::
    //CoHandler(ViewPath("/boatList"))(boatListView) ::
    CoHandler(ViewPath("/entryList"))(entryListView) ::
    CoHandler(ViewPath("/entryEdit"))(entryEditView) ::
    onUpdate.handlers(List(logAt.asWork,logAt.workStart,logAt.workStop).map(attrFactory.attrId(_)), calcWorkDuration) :::
    onUpdate.handlers(List(logAt.asWork,logAt.workDuration,logAt.entryOfWork).map(attrFactory.attrId(_)), calcEntryDuration) :::
    onUpdate.handlers(List(logAt.asEntry,logAt.confirmedOn).map(attrFactory.attrId(_)), setEntryConfirmDate) :::
    CoHandler(SessionInstantProbablyAdded)(currentVDom.invalidate) ::
    Nil
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



