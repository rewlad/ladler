
package ee.cone.base.framework

import ee.cone.base.connection_api.{Attr, FieldAttributes, Obj, ObjId}
import ee.cone.base.db._
import ee.cone.base.flexlayout._
import ee.cone.base.material._
import ee.cone.base.vdom._
import ee.cone.base.vdom.Types._

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
  htmlTable: TableTags,
  tableIconTags: TableIconTags,
  fields: Fields
) {
  import divTags._
  import materialTags._
  import flexTags._
  import htmlTable._
  import tableIconTags._
  import fieldAttributes.aIsEditing
  import buttonTags._
  import uiStrings.caption

  //listAttrs, editing.reset, alien.demanded, alien.wrapForUpdate
  def creationTimeOrdering =
    objOrderingFactory.ordering(alienAttributes.createdAt, reverse = true).get
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
    paperWithMargin(key,
      flexGrid("flexGrid")(List(
        flexItem("widthSync",1000/*temp*/,None)((widthAttr:List[TagAttr])⇒
          controls ::: table("1",widthAttr:_*)(tableElements)
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
}
