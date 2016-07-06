
package ee.cone.base.test_loots // to app

import ee.cone.base.connection_api.{Attr, FieldAttributes, Obj, ObjId}
import ee.cone.base.db._
import ee.cone.base.flexlayout._
import ee.cone.base.material._
import ee.cone.base.util.Setup
import ee.cone.base.vdom._
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

case object MaterialFont extends TagStyle {
  def appendStyle(builder: JsonBuilder) = {
    //builder.append("fontSize").append(if(isHeader) "12px" else "13px")
    builder.append("fontWeight").append("500")
    builder.append("color").append("rgba(0,0,0,0.54)")
  }
}

class ItemList(
    val listed: ObjCollection, val list: List[Obj], val selection: ObjSelection,
    val isEditable: Boolean
)


class MaterialDataTableUtils(
  objOrderingFactory: ObjOrderingFactory,
  uiStrings: UIStrings,
  listAttrs: ObjSelectionAttributes,
  editing: Editing,
  selectedFactory: ObjSelectionFactory,
  alien: Alien,
  alienAttributes: AlienAttributes,

  fieldAttributes: FieldAttributes,
  style: TagStyles,
  divTags: Tags,
  buttonTags: ButtonTags,
  materialTags: MaterialTags,
  flexTags: FlexTags,
  dtTablesState: DataTablesState,
  htmlTable: TableTags,
  tableIconTags: TableIconTags,
  fields: Fields
) {
  import divTags._
  import materialTags._
  import flexTags._
  import htmlTable._
  import tableIconTags._
  import uiStrings.caption
  import fieldAttributes.aIsEditing
  import buttonTags._

  def createItemList[Value](
      theListed: ObjCollection,
      filterObj: Obj,
      filters: List[Obj⇒Boolean],
      editable: Boolean
  ) = {
    val selection = selectedFactory.create(filterObj)
    val items = theListed.toList
      .filter(obj⇒filters.forall(_(obj)))
      .map(selection.wrap)
      .map(obj⇒if(editable) editing.wrap(obj) else obj)
    new ItemList(theListed, items, selection, editable)
  }

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
      flexGrid("flexGrid")(List(
        flexItem("widthSync",1000/*temp*/,None,onResize=Some(w⇒tableWidth.value=w.toFloat))(
          controls :::
          List(div("table",MaterialFont)(
            table("1",Width(tableWidth.value))(tableElements)
          ))
        )
      ))
    )
  }

  def selectAllCheckBox(itemList: ItemList) = {
    val selected = itemList.selection.collection.toList
    List(
      checkBox("1","",
        selected.nonEmpty,
        on ⇒
          if(on) itemList.selection.collection.add(itemList.list)
          else itemList.selection.collection.remove(selected)
      )
    )
  }

  def header(attr: Attr[_]):List[ChildPair[OfDiv]] = List(text("1",caption(attr)))
  def sortingHeader(itemListOrdering: ItemListOrdering, attr: Attr[_]):List[ChildPair[OfDiv]] = {
    val (action,reversed) = itemListOrdering.action(attr)
    if(action.isEmpty) header(attr) else {
      val icon = (reversed match {
        case None ⇒ List()
        case Some(false) ⇒ iconArrowDown :: Nil
        case Some(true) ⇒ iconArrowUp :: Nil
      }).map(name⇒tag("icon",name,style.alignMiddle)(Nil))
      List(divButton("1")(action.get)(icon:::header(attr)))
    }
  }

  def removeControlView(itemList: ItemList) =
    if(!itemList.isEditable) Nil else
      List(iconButton("btnDelete", "delete", iconDelete){ ()⇒
        val selected = itemList.selection.collection.toList.map(alien.wrapForUpdate)
        itemList.listed.remove(selected)
        itemList.selection.collection.remove(selected)
      })

  def addRemoveControlViewBase(itemList: ItemList)(add: Obj⇒Unit) =
    removeControlView(itemList) ::: (if(!itemList.isEditable) Nil else {
      val newItem = alien.demanded(_⇒())
      List(iconButton("btnAdd", "add", iconAdd){ () ⇒
        itemList.listed.add(List(newItem))
        add(newItem)
      })
    })

  def addRemoveControlView(itemList: ItemList) =
    addRemoveControlViewBase(itemList: ItemList){ obj ⇒
      val item = itemList.selection.wrap(obj)
      item(aIsEditing) = true
      item(listAttrs.isExpanded) = true
    }


  private def iconCellGroup(key: VDomKey)(content: Boolean⇒List[ChildPair[OfDiv]]): List[ChildOfTableRow] = List(
    group(s"${key}_grp", MinWidth(50), MaxWidth(50), Priority(1), style.alignCenter)(Nil),
    cell(key, MinWidth(50))(content)
  )

  def selectAllGroup(itemList: ItemList) =
    iconCellGroup("selected")(_⇒selectAllCheckBox(itemList))
  def selectRowGroup(item: Obj) =
    iconCellGroup("selected")(_⇒fields.field(item, listAttrs.isSelected, showLabel=false, EditableFieldOption(true)))
  def editAllGroup() = iconCellGroup("edit")(_⇒Nil)
  def editRowGroupBase(on: Boolean)(action: ()⇒Unit) =
    iconCellGroup("edit")(_⇒if(on) List(iconButton("btnCreate","edit",iconModeEdit)(action)) else Nil)
  def editRowGroup(itemList: ItemList, item: Obj) =
    editRowGroupBase(itemList.isEditable){ () ⇒
      item(aIsEditing) = !item(aIsEditing)
      item(listAttrs.isExpanded) = true
    }

  def creationTimeOrdering =
    objOrderingFactory.ordering(alienAttributes.createdAt, reverse = true).get

  def controlPanel(chld1:List[ChildPair[OfDiv]],chld2:List[ChildPair[OfDiv]]): List[ChildPair[OfDiv]] =
    List(div("tableControl",style.padding(5))(List(div("1",paddingSide(8))(List(
      div("1",style.displayInlineBlock,style.minWidth(1),style.maxWidth(1),style.minHeight(48))(Nil),
      div("2",style.displayInlineBlock,OnlyWidthPercentTagStyle(60))(chld1),
      div("3",RightFloatTagStyle)(chld2)
    )))))
}

case class OnlyWidthPercentTagStyle(value: Int) extends TagStyle {
  def appendStyle(builder: JsonBuilder) = {
    builder.append("minWidth").append(s"$value%")
    builder.append("maxWidth").append(s"$value%")
  }
}

case object RightFloatTagStyle extends TagStyle {
  def appendStyle(builder: JsonBuilder) =
    builder.append("float").append("right")
}