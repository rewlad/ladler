package ee.cone.base.db

import ee.cone.base.connection_api._

import ee.cone.base.util.Never


class UIStringAttributes(
  attr: AttrFactory,
  asString: AttrValueType[String]
)(
  val caption: Attr[String] = attr("2aec9be5-72b4-4983-b458-4f95318bfd2a", asString)
)

class UIStringsImpl(
  at: UIStringAttributes,
  handlerLists: CoHandlerLists,
  attrFactory: AttrFactory,
  factIndex: FactIndex,
  onUpdate: OnUpdate,
  findNodes: FindNodes,
  asDBObj: AttrValueType[Obj],

  asObjId: AttrValueType[ObjId],
  asString: AttrValueType[String]
) extends UIStrings with CoHandlerProvider {
  def handlers = factIndex.handlers(at.caption) :::
    List(
      CoHandler(ToUIStringConverter(asDBObj,asString))(objToUIString),
      CoHandler(ToUIStringConverter(asObjId,asString))(objIdToUIString)
    )
  private def objIdToUIString(value: ObjId) = objToUIString(findNodes.whereObjId(value))
  private def objToUIString(obj: Obj) = {
    val res = obj(at.caption)
    if(res.nonEmpty) res else obj.toString
  }
  def handlers(attributes: List[Attr[_]])(calculate: Obj⇒String) =
    onUpdate.handlers(attributes.map(attrFactory.attrId(_)), (on,obj)⇒
      obj(at.caption) = if(on) calculate(obj) else ""
    )

  def caption(attr: Attr[_]) =
    handlerLists.single(AttrCaption(attr), ()⇒attrFactory.attrId(attr).toString)

  def converter[From,To](from: AttrValueType[From], to: AttrValueType[To]) =
    handlerLists.single(ToUIStringConverter(from,to),
      ()⇒(v:From)⇒if(to==asString) v.toString.asInstanceOf[To] else Never()
    )
}