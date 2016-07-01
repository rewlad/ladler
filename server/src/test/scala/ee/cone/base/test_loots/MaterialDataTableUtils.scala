package ee.cone.base.test_loots

import ee.cone.base.connection_api.{Attr, Obj, ObjId}
import ee.cone.base.db.{ItemListOrdering, _}
import ee.cone.base.vdom.{ChildPair, CurrentVDom, OfDiv}
import ee.cone.base.vdom.Types._

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

class MaterialDataTableUtils(
  objOrderingFactory: ObjOrderingFactory,
  uiStrings: UIStrings,
  listAttrs: ItemListAttributes,
  editing: Editing,

  fieldAttributes: FieldAttributes,
  style: TagStyles,
  divTags: DivTags,
  materialTags: MaterialTags,
  flexTags: FlexTags,
  dtTablesState: DataTablesState,
  htmlTable: TableTags,
  materialIconTags: MaterialIconTags,
  fields: Fields
) {
  import divTags._
  import materialTags._
  import flexTags._
  import htmlTable._
  import materialIconTags._
  import uiStrings.caption
  import fieldAttributes.aIsEditing

  def toggledSelectedRow(item: Obj) = List(
    Toggled(item(listAttrs.isExpanded))(Some(()=> if(!item(listAttrs.isExpanded)){
      editing.reset()
      item(listAttrs.isExpanded) = true
    })),
    IsSelected(item(listAttrs.isSelected))
  )
  def toggledRow(filterObj: Obj, id: ObjId) = List(
    Toggled(filterObj(listAttrs.expandedItem)==id)(Some(()=>filterObj(listAttrs.expandedItem)=id))
  )

  def paperTable(key: VDomKey)(controls:List[ChildPair[OfDiv]],tableElements: List[ChildOfTable]): ChildPair[OfDiv] = {
    val tableWidth = dtTablesState.widthOfTable(key)
    paperWithMargin(key,
      flexGrid("flexGrid",
        flexGridItemWidthSync("widthSync",w⇒tableWidth.value=w.toFloat,
          controls:::
            table("1",List(Width(tableWidth.value)))(tableElements)
        )
      )
    )
  }

  def selectAllCheckBox(itemList: ItemList) = List(
    checkBox("1","",
      itemList.filter(listAttrs.selectedItems).nonEmpty,
      on ⇒
        if(on) itemList.selectAllListed()
        else itemList.filter(listAttrs.selectedItems)=Set[ObjId]()
    )
  )

  def header(attr: Attr[_]):List[ChildPair[OfDiv]] = List(text("1",caption(attr)))
  def sortingHeader(itemListOrdering: ItemListOrdering, attr: Attr[_]):List[ChildPair[OfDiv]] = {
    val (action,reversed) = itemListOrdering.action(attr)
    if(action.isEmpty) header(attr) else {
      val icon = reversed match {
        case None ⇒ List()
        case Some(false) ⇒ iconArrowDown()::Nil
        case Some(true) ⇒ iconArrowUp()::Nil
      }
      List(divTags.divButton("1")(action.get)(icon:::header(attr)))
    }
  }

  private def removeControlView(itemList: ItemList) =
    if(itemList.isEditable) List(btnDelete("btnDelete", itemList.removeSelected)) else Nil

  def addRemoveControlViewBase(itemList: ItemList)(add: ()⇒Unit) =
    if(itemList.isEditable) List(btnDelete("btnDelete", itemList.removeSelected),btnAdd("btnAdd", add))
    else Nil
  def addRemoveControlView(itemList: ItemList) =
    addRemoveControlViewBase(itemList: ItemList){ ()⇒
      val item = itemList.add()
      item(aIsEditing) = true
      item(listAttrs.isExpanded) = true
    }


  private def iconCellGroup(key: VDomKey)(content: Boolean⇒List[ChildPair[OfDiv]]) = List(
    group(s"${key}_grp", MinWidth(50), MaxWidth(50), Priority(1), style.alignCenter),
    cell(key, MinWidth(50))(content)
  )

  def selectAllGroup(itemList: ItemList) =
    iconCellGroup("selected")(_⇒selectAllCheckBox(itemList))
  def selectRowGroup(item: Obj) =
    iconCellGroup("selected")(_⇒fields.field(item, listAttrs.isSelected, showLabel=false, EditableFieldOption(true)))
  def editAllGroup() = iconCellGroup("edit")(_⇒Nil)
  def editRowGroupBase(on: Boolean)(action: ()⇒Unit) =
    iconCellGroup("edit")(_⇒if(on) List(btnModeEdit("btnCreate",action)) else Nil)
  def editRowGroup(itemList: ItemList, item: Obj) =
    editRowGroupBase(itemList.isEditable){ () ⇒
      item(aIsEditing) = !item(aIsEditing)
      item(listAttrs.isExpanded) = true
    }

  def creationTimeOrdering =
    objOrderingFactory.ordering(listAttrs.createdAt, reverse = true).get

  def controlPanel(chld1:List[ChildPair[OfDiv]],chld2:List[ChildPair[OfDiv]]): List[ChildPair[OfDiv]] =
    List(div("tableControl",style.padding(5))(List(div("1",style.paddingSide(8))(List(
      div("1",style.displayInlineBlock,style.minWidth(1),style.maxWidth(1),style.minHeight(48))(Nil),
      div("2",style.displayInlineBlock,style.minWidthPercent(60),style.maxWidthPercent(60))(chld1),
      div("3",style.floatRight)(chld2)
    )))))
}
