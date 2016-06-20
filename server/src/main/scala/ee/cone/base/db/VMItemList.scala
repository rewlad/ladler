package ee.cone.base.db

import java.time.Instant

import ee.cone.base.connection_api._
import ee.cone.base.util.Setup

class ListedWrapType extends WrapType[InnerItemList]

trait InnerItemList {
  def get(obj: Obj, attr: Attr[Boolean]): Boolean
  def set(obj: Obj, attr: Attr[Boolean], value: Boolean): Unit
}

class ItemListAttributesImpl(
  attr: AttrFactory,
  asBoolean: AttrValueType[Boolean],
  asObjId: AttrValueType[ObjId],
  asObjIdSet: AttrValueType[Set[ObjId]],
  asInstant: AttrValueType[Option[Instant]]
)(
  val isSelected: Attr[Boolean] = attr("a5045617-279f-48b8-95a9-a42dc721d67b",asBoolean),
  val isExpanded: Attr[Boolean] = attr("2dd74df5-0ca7-4734-bc49-f420515fd663",asBoolean),
  val selectedItems: Attr[Set[ObjId]] = attr("32a62c43-e837-4855-985a-d79f5dc03db0",asObjIdSet),
  val expandedItem: Attr[ObjId] = attr("d0b7b274-74ac-40b0-8e51-a1e1751578af", asObjId),
  val createdAt: Attr[Option[Instant]] = attr("8b9fb96d-76e5-4db3-904d-1d18ff9f029d",asInstant)
) extends ItemListAttributes

class ItemListFactoryImpl(
  at: ItemListAttributes, nodeAttrs: NodeAttrs, findAttrs: FindAttrs, alienAttrs: AlienAccessAttrs,
  attrFactory: AttrFactory, findNodes: FindNodes, mainTx: CurrentTx[MainEnvKey],
  alien: Alien, listedWrapType: WrapType[InnerItemList], factIndex: FactIndex,
  transient: Transient, objIdFactory: ObjIdFactory, lazyObjFactory: LazyObjFactory,
  editing: Editing
) extends ItemListFactory with CoHandlerProvider {
  def create[Value](
    index: SearchByLabelProp[Value],
    parentValue: Value,
    filterObj: Obj,
    filters: List[Obj⇒Boolean],
    editable: Boolean
  ): ItemList = {
    val parentAttr = attrFactory.toAttr(index.propId, index.propType)
    val asType = attrFactory.toAttr(index.labelId, index.labelType)
    val selectedSet = filterObj(at.selectedItems)
    val expandedItem = filterObj(at.expandedItem)
    val getElement = Map[Attr[Boolean],Obj⇒Boolean](
      at.isSelected → { obj ⇒ selectedSet contains obj(nodeAttrs.objId) },
      at.isExpanded → { obj ⇒ expandedItem == obj(nodeAttrs.objId) }
    )
    val setElement = Map[(Attr[Boolean],Boolean),Obj⇒Unit](
      (at.isSelected→false) → { obj ⇒ filterObj(at.selectedItems) = selectedSet - obj(nodeAttrs.objId) },
      (at.isSelected→true)  → { obj ⇒ filterObj(at.selectedItems) = selectedSet + obj(nodeAttrs.objId) },
      (at.isExpanded→false) → { obj ⇒ filterObj(at.expandedItem) = objIdFactory.noObjId },
      (at.isExpanded→true)  → { obj ⇒ filterObj(at.expandedItem) = obj(nodeAttrs.objId) }
    )
    val inner = new InnerItemList {
      def get(obj: Obj, attr: Attr[Boolean]) = getElement(attr)(obj)
      def set(obj: Obj, attr: Attr[Boolean], value: Boolean) = setElement((attr,value))(obj)
    }
    val items =
      findNodes.where(mainTx(), index, parentValue, Nil)
        .filter(obj⇒filters.forall(_(obj)))
        .map(obj⇒if(editable) editing.wrap(obj) else obj)
        .map(obj⇒ obj.wrap(listedWrapType,inner))

    def setupNew(obj: Obj) = {
      obj(asType) = obj
      obj(at.createdAt) = Option(Instant.now())
    }
    val newItem = alien.demandedNode(setupNew).wrap(listedWrapType,inner)
    new ItemList {
      def filter = filterObj
      def list = items
      def add() = Setup(newItem)(_(parentAttr) = parentValue)
      def removeSelected() = {
        val empty = attrFactory.converter(attrFactory.valueType(parentAttr)).convertEmpty()
        selectedSet.map(findNodes.whereObjId).map(alien.wrapForEdit)
          .foreach(obj⇒obj(parentAttr) = empty)
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
      List(at.isSelected, at.isExpanded).flatMap{ attr ⇒ List(
        CoHandler(GetValue(listedWrapType,attr)){ (obj,innerObj)⇒
          innerObj.data.get(obj,attr)
        },
        CoHandler(SetValue(listedWrapType,attr)){ (obj,innerObj,value)⇒
          innerObj.data.set(obj,attr,value)
        }
      )} :::
      List(at.selectedItems, at.createdAt).flatMap(alien.update(_)) :::
      List(at.expandedItem).flatMap{ attr ⇒ transient.update(attr) }
}
