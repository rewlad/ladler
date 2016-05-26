package ee.cone.base.test_loots

import java.util.UUID

import ee.cone.base.connection_api._
import ee.cone.base.db._
import ee.cone.base.util.{Setup, Never}

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
  val isExpanded: Attr[Boolean] = attr("2dd74df5-0ca7-4734-bc49-f420515fd663",asBoolean),
  val selectedItems: Attr[Set[ObjId]] = attr("32a62c43-e837-4855-985a-d79f5dc03db0",asObjIdSet),
  val expandedItem: Attr[String] = attr("d0b7b274-74ac-40b0-8e51-a1e1751578af", asString)
)

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
  findAttrs: FindAttrs,
  alienAttrs: AlienAccessAttrs,
  handlerLists: CoHandlerLists,
  attrFactory: AttrFactory,
  findNodes: FindNodes,
  mainTx: CurrentTx[MainEnvKey],
  alien: Alien,
  listedWrapType: WrapType[InnerItemList],
  factIndex: FactIndex,
  searchIndex: SearchIndex,
  transient: Transient
)(
  val filterByFullKey: SearchByLabelProp[String] = searchIndex.create(at.asFilter,at.filterFullKey)
) extends CoHandlerProvider {
  private def eventSource = handlerLists.single(SessionEventSource, ()⇒Never())
  def lazyLinkingObj[Value](index: SearchByLabelProp[Value], key: Value): Obj = {
    val obj = findNodes.single(findNodes.where(mainTx(), index, key, Nil))
    if(obj(findAttrs.nonEmpty)) alien.wrap(obj) else alien.demandedNode { obj ⇒
      obj(attrFactory.toAttr(index.labelId, index.labelType)) = obj
      obj(attrFactory.toAttr(index.propId, index.propType)) = key
    }
  }
  def filterObj(key: String): Obj =
    lazyLinkingObj(filterByFullKey, s"${alien.wrap(eventSource.mainSession)(alienAttrs.objIdStr)}$key")
  def itemList[Value](
    index: SearchByLabelProp[Value],
    parentValue: Value,
    filterObj: Obj
  ): ItemList = {
    val selectedSet = filterObj(at.selectedItems)
    val parentAttr = attrFactory.toAttr(index.propId, index.propType)
    val asType = attrFactory.toAttr(index.labelId, index.labelType)
    val expandedItem = filterObj(at.expandedItem)
    val getElement = Map[Attr[Boolean],Obj⇒Boolean](
      at.isSelected → { obj ⇒ selectedSet contains obj(nodeAttrs.objId) },
      at.isListed → { obj ⇒ obj(parentAttr) == parentValue },
      at.isExpanded → { obj ⇒ expandedItem == obj(alienAttrs.objIdStr) }
    )
    val setElement = Map[(Attr[Boolean],Boolean),Obj⇒Unit](
      (at.isSelected→false) → { obj ⇒ filterObj(at.selectedItems) = selectedSet - obj(nodeAttrs.objId) },
      (at.isSelected→true)  → { obj ⇒ filterObj(at.selectedItems) = selectedSet + obj(nodeAttrs.objId) },
      (at.isListed→false)   → { obj ⇒ obj(parentAttr) = attrFactory.converter(attrFactory.valueType(parentAttr)).convertEmpty() },
      (at.isListed→true)    → { obj ⇒ obj(parentAttr) = parentValue },
      (at.isExpanded→false) → { obj ⇒ filterObj(at.expandedItem) = "" },
      (at.isExpanded→true)  → { obj ⇒ filterObj(at.expandedItem) = obj(alienAttrs.objIdStr) }
    )
    val inner = new InnerItemList {
      def get(obj: Obj, attr: Attr[Boolean]) = getElement(attr)(obj)
      def set(obj: Obj, attr: Attr[Boolean], value: Boolean) = setElement((attr,value))(obj)
    }

    val items = findNodes.where(mainTx(), index, parentValue, Nil)
      .map(obj⇒
        alien.wrap(obj).wrap(listedWrapType,inner)
      )
    val newItem = alien.demandedNode{ obj ⇒ obj(asType) = obj }.wrap(listedWrapType,inner)
    new ItemList {
      def filter = filterObj
      def list = items
      def add() = Setup(newItem)(_(at.isListed) = true)
      def removeSelected() = {
        selectedSet.foreach(objId⇒
          alien.wrap(findNodes.whereObjId(objId)).wrap(listedWrapType,inner)(at.isListed)=false
        )
        filter(at.selectedItems) = Set[ObjId]()
      }
      def selectAllListed() =
        filter(at.selectedItems) = selectedSet ++ items.map(_(nodeAttrs.objId))
    }
  }

  def handlers =
    List(at.isSelected, at.isListed, at.isExpanded).flatMap{ attr ⇒ List(
      CoHandler(GetValue(listedWrapType,attr)){ (obj,innerObj)⇒
        innerObj.data.get(obj,attr)
      },
      CoHandler(SetValue(listedWrapType,attr)){ (obj,innerObj,value)⇒
        innerObj.data.set(obj,attr,value)
      }
    )} :::
    List(at.asFilter, at.filterFullKey, at.selectedItems).flatMap{ attr⇒
      factIndex.handlers(attr) ::: alien.update(attr)
    } :::
    List(at.expandedItem).flatMap{ attr ⇒ transient.update(attr) } :::
    searchIndex.handlers(filterByFullKey)
}

case object TransientChanged extends EventKey[()=>Unit]
class Transient(handlerLists: CoHandlerLists, attrFactory: AttrFactory, wrapType: WrapType[ObjId]) {
  def update[R](attr: Attr[R]): List[BaseCoHandler] = {
    val data = collection.mutable.Map[ObjId,R]()
    List(
      CoHandler(GetValue(wrapType,attr))((obj,innerObj)⇒
        data.getOrElse(innerObj.data, attrFactory.converter(attrFactory.valueType(attr)).convertEmpty())
      ),
      CoHandler(SetValue(wrapType,attr)) { (obj, innerObj, value) ⇒
        data(innerObj.data) = value
        handlerLists.list(TransientChanged).foreach(_())
      }
    )
  }
}