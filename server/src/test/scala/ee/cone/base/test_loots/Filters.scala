package ee.cone.base.test_loots

import java.time.Instant

import ee.cone.base.connection_api._
import ee.cone.base.db._
import ee.cone.base.util.{Never, Setup}

trait InnerItemList {
  def get(obj: Obj, attr: Attr[Boolean]): Boolean
  def set(obj: Obj, attr: Attr[Boolean], value: Boolean): Unit
}

trait ItemList {
  def filter: Obj
  def add(): Obj
  def list: List[Obj]
  def selectAllListed(): Unit
  def removeSelected(): Unit
  def isEditable: Boolean
}

class ListItemAttrs(
  attr: AttrFactory,
  asBoolean: AttrValueType[Boolean],
  asObjId: AttrValueType[ObjId],
  asObjIdSet: AttrValueType[Set[ObjId]],
  asInstant: AttrValueType[Option[Instant]]
)(
  val isSelected: Attr[Boolean] = attr("a5045617-279f-48b8-95a9-a42dc721d67b",asBoolean),
  val isListed: Attr[Boolean] = attr("bd68ccbc-b63c-45ce-88f2-7c6058b11338",asBoolean),
  val isExpanded: Attr[Boolean] = attr("2dd74df5-0ca7-4734-bc49-f420515fd663",asBoolean),
  val selectedItems: Attr[Set[ObjId]] = attr("32a62c43-e837-4855-985a-d79f5dc03db0",asObjIdSet),
  val expandedItem: Attr[ObjId] = attr("d0b7b274-74ac-40b0-8e51-a1e1751578af", asObjId),
  val createdAt: Attr[Option[Instant]] = attr("8b9fb96d-76e5-4db3-904d-1d18ff9f029d",asInstant)
)

trait ItemListFactory {
  def create[Value](
    index: SearchByLabelProp[Value],
    parentValue: Value,
    filterObj: Obj,
    filters: List[Obj⇒Boolean],
    editable: Boolean
  ): ItemList
}

class ItemListFactoryImpl(
  at: ListItemAttrs,
  nodeAttrs: NodeAttrs,
  findAttrs: FindAttrs,
  alienAttrs: AlienAccessAttrs,
  attrFactory: AttrFactory,
  findNodes: FindNodes,
  mainTx: CurrentTx[MainEnvKey],
  alien: Alien,
  listedWrapType: WrapType[InnerItemList],
  factIndex: FactIndex,

  transient: Transient,
  objIdFactory: ObjIdFactory,
  lazyObjFactory: LazyObjFactory
)(

  var editing: Obj = findNodes.noNode
) extends ItemListFactory with CoHandlerProvider {
  def itemList[Value](
    index: SearchByLabelProp[Value],
    parentValue: Value,
    filterObj: Obj,
    filters: List[Obj⇒Boolean],
    editable: Boolean
  ): ItemList = {
    val selectedSet = filterObj(at.selectedItems)
    val parentAttr = attrFactory.toAttr(index.propId, index.propType)
    val asType = attrFactory.toAttr(index.labelId, index.labelType)
    val expandedItem = filterObj(at.expandedItem)
    val getElement = Map[Attr[Boolean],Obj⇒Boolean](
      at.isSelected → { obj ⇒ selectedSet contains obj(nodeAttrs.objId) },
      at.isListed → { obj ⇒ obj(parentAttr) == parentValue },
      at.isExpanded → { obj ⇒ expandedItem == obj(nodeAttrs.objId) }
    )
    val setElement = Map[(Attr[Boolean],Boolean),Obj⇒Unit](
      (at.isSelected→false) → { obj ⇒ filterObj(at.selectedItems) = selectedSet - obj(nodeAttrs.objId) },
      (at.isSelected→true)  → { obj ⇒ filterObj(at.selectedItems) = selectedSet + obj(nodeAttrs.objId) },
      (at.isListed→false)   → { obj ⇒ obj(parentAttr) = attrFactory.converter(attrFactory.valueType(parentAttr)).convertEmpty() },
      (at.isListed→true)    → { obj ⇒ obj(parentAttr) = parentValue },
      (at.isExpanded→false) → { obj ⇒ filterObj(at.expandedItem) = objIdFactory.noObjId },
      (at.isExpanded→true)  → { obj ⇒ filterObj(at.expandedItem) = obj(nodeAttrs.objId) }
    )
    val inner = new InnerItemList {
      def get(obj: Obj, attr: Attr[Boolean]) = getElement(attr)(obj)
      def set(obj: Obj, attr: Attr[Boolean], value: Boolean) = setElement((attr,value))(obj)
    }

    val editingId = editing(nodeAttrs.objId)

    val items =
      findNodes.where(mainTx(), index, parentValue, Nil)
      .filter(obj⇒filters.forall(_(obj)))
      .map(obj⇒if(editable && obj(nodeAttrs.objId) == editingId) alien.wrapForEdit(obj) else obj)
      .map(obj⇒ obj.wrap(listedWrapType,inner))

    def setupNew(obj: Obj) = {
      obj(asType) = obj
      obj(at.createdAt) = Option(Instant.now())
    }
    val newItem = alien.demandedNode(setupNew).wrap(listedWrapType,inner)

    new ItemList {
      def filter = filterObj
      def list = items
      def add() = Setup(newItem)(_(at.isListed) = true)
      def removeSelected() = {
        selectedSet.foreach(objId⇒
          alien.wrapForEdit(findNodes.whereObjId(objId)).wrap(listedWrapType,inner)(at.isListed)=false
        )
        filter(at.selectedItems) = Set[ObjId]()
      }
      def selectAllListed() =
        filter(at.selectedItems) = selectedSet ++ items.map(_(nodeAttrs.objId))
      def isEditable = editable
    }
  }

  def handlers =
    CoHandler(AttrCaption(at.selectedItems))("Selected Items") ::
    CoHandler(AttrCaption(at.createdAt))("Creation Time") ::
    List(at.isSelected, at.isListed, at.isExpanded).flatMap{ attr ⇒ List(
      CoHandler(GetValue(listedWrapType,attr)){ (obj,innerObj)⇒
        innerObj.data.get(obj,attr)
      },
      CoHandler(SetValue(listedWrapType,attr)){ (obj,innerObj,value)⇒
        innerObj.data.set(obj,attr,value)
      }
    )} :::
    CoHandler(SetValue(listedWrapType,alienAttrs.isEditing)){ (obj,innerObj,value)⇒
      if(value) editing = obj
      else if(obj(alienAttrs.isEditing)) editing = findNodes.noNode
    } ::
    List(at.selectedItems, at.createdAt).flatMap{ attr⇒
      factIndex.handlers(attr) ::: alien.update(attr)
    } :::
    List(at.expandedItem).flatMap{ attr ⇒ transient.update(attr) }
}

