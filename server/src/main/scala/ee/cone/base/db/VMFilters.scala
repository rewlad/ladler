package ee.cone.base.db

import ee.cone.base.connection_api._
import ee.cone.base.util.Never


class FilterAttrs(
  attr: AttrFactory,
  label: LabelFactory,
  asObjId: AttrValueType[ObjId]
)(
  val asFilter: Attr[Obj] = label("eee2d171-b5f2-4f8f-a6d9-e9f3362ff9ed"),
  val filterFullKey: Attr[ObjId] = attr("2879097b-1fd6-45b1-a8b4-1de807ce9572",asObjId)
)

class FilterObjFactoryImpl(
  at: FilterAttrs,
  nodeAttrs: NodeAttrs,
  handlerLists: CoHandlerLists,
  factIndex: FactIndex,
  searchIndex: SearchIndex,
  alien: Alien,
  lazyObjFactory: LazyObjFactory
)(
  val filterByFullKey: SearchByLabelProp[ObjId] = searchIndex.create(at.asFilter,at.filterFullKey)
) extends FilterObjFactory with CoHandlerProvider {
  private def eventSource = handlerLists.single(SessionEventSource, ()⇒Never())
  def create(ids: List[ObjId]) = lazyObjFactory.create(
    filterByFullKey,
    eventSource.mainSession(nodeAttrs.objId) :: ids,
    wrapForEdit = true
  )
  def handlers =
    CoHandler(AttrCaption(at.asFilter))("View Model") ::
      CoHandler(AttrCaption(at.filterFullKey))("Key") ::
      List(at.asFilter, at.filterFullKey).flatMap{ attr⇒
        factIndex.handlers(attr) ::: alien.update(attr)
      } :::
      searchIndex.handlers(filterByFullKey)
}

