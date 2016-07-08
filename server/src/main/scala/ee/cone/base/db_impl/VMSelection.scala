package ee.cone.base.db_impl

import java.time.Instant
import ee.cone.base.connection_api._

class ListedWrapType extends WrapType[InnerItemList]

trait InnerItemList {
  def get(obj: Obj, attr: Attr[Boolean]): Boolean
  def set(obj: Obj, attr: Attr[Boolean], value: Boolean): Unit
}

class ObjSelectionAttributesImpl(
    attr: AttrFactory,
    asBoolean: AttrValueType[Boolean],
    asObjId: AttrValueType[ObjId],
    asObjIdSet: AttrValueType[Set[ObjId]],
    asInstant: AttrValueType[Option[Instant]]
)(
    val isSelected: Attr[Boolean] = attr("a5045617-279f-48b8-95a9-a42dc721d67b",asBoolean),
    val isExpanded: Attr[Boolean] = attr("2dd74df5-0ca7-4734-bc49-f420515fd663",asBoolean),
    val selectedItems: Attr[Set[ObjId]] = attr("32a62c43-e837-4855-985a-d79f5dc03db0",asObjIdSet),
    val expandedItem: Attr[ObjId] = attr("d0b7b274-74ac-40b0-8e51-a1e1751578af", asObjId)
) extends ObjSelectionAttributes

class ObjSelectionFactoryImpl(
    at: ObjSelectionAttributesImpl, nodeAttrs: NodeAttrs, objIdFactory: ObjIdFactory,
    findNodes: FindNodes, transient: Transient, alien: Alien,
    listedWrapType: WrapType[InnerItemList]
) extends ObjSelectionFactory with CoHandlerProvider {
  def create(filterObj: Obj) = {
    val selectedSet = filterObj(at.selectedItems)
    val expandedItem = filterObj(at.expandedItem)
    val getElement = Map[Attr[Boolean],Obj⇒Boolean](
      at.isSelected → { obj ⇒ selectedSet contains obj(nodeAttrs.objId) },
      at.isExpanded → { obj ⇒ expandedItem == obj(nodeAttrs.objId) }
    )
    val theCollection = new ObjCollection {
      def toList: List[Obj] = selectedSet.map(findNodes.whereObjId).toList // not sorted
      def remove(list: List[Obj]) =
        filterObj(at.selectedItems) = selectedSet -- list.map(_(nodeAttrs.objId))
      def add(list: List[Obj]) =
        filterObj(at.selectedItems) = selectedSet ++ list.map(_(nodeAttrs.objId))
    }
    val setElement = Map[(Attr[Boolean],Boolean),Obj⇒Unit](
      (at.isSelected→false) → { obj ⇒ theCollection.remove(List(obj)) },
      (at.isSelected→true)  → { obj ⇒ theCollection.add(List(obj)) },
      (at.isExpanded→false) → { obj ⇒ filterObj(at.expandedItem) = objIdFactory.noObjId },
      (at.isExpanded→true)  → { obj ⇒ filterObj(at.expandedItem) = obj(nodeAttrs.objId) }
    )
    val inner = new InnerItemList {
      def get(obj: Obj, attr: Attr[Boolean]) = getElement(attr)(obj)
      def set(obj: Obj, attr: Attr[Boolean], value: Boolean) = setElement((attr,value))(obj)
    }
    new ObjSelection {
      def collection = theCollection
      def wrap(obj: Obj) = obj.wrap(listedWrapType,inner)
    }
  }
  def handlers =
    CoHandler(AttrCaption(at.selectedItems))("Selected Items") ::
      List(at.isSelected, at.isExpanded).flatMap{ attr ⇒ List(
        CoHandler(GetValue(listedWrapType,attr)){ (obj,innerObj)⇒
          innerObj.data.get(obj,attr)
        },
        CoHandler(SetValue(listedWrapType,attr)){ (obj,innerObj,value)⇒
          innerObj.data.set(obj,attr,value)
        }
      )} :::
      alien.update(at.selectedItems) :::
      transient.update(at.expandedItem)
}
