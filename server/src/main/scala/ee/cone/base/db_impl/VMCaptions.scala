package ee.cone.base.db_impl

import java.util.UUID

import ee.cone.base.connection_api._
import ee.cone.base.db.{ObjIdCaption, UIStrings, OnUpdate, NodeAttrs}
import ee.cone.base.util.Never

class UIStringAttributes(
  attr: AttrFactoryI,
  asString: AttrValueType[String]
)(
  val caption: Attr[String] = attr("2aec9be5-72b4-4983-b458-4f95318bfd2a", asString)
)

class UIStringsImpl(
  at: UIStringAttributes,
  nodeAttrs: NodeAttrs,
  handlerLists: CoHandlerLists,
  objIdFactory: ObjIdFactoryI,
  attrFactory: AttrFactoryI,
  factIndex: FactIndexI,
  onUpdate: OnUpdate,
  findNodes: FindNodesI,
  asDBObj: AttrValueType[Obj],
  asObjId: AttrValueType[ObjId],
  asString: AttrValueType[String],
  asUUID: AttrValueType[Option[UUID]]
) extends UIStrings with CoHandlerProvider {
  def handlers = factIndex.handlers(at.caption) :::
    List(
      CoHandler(ObjIdCaption(objIdFactory.noObjId))("(None)"),
      CoHandler(ConverterKey(asDBObj,asString))(objToUIString),
      CoHandler(ConverterKey(asObjId,asString))(objIdToUIString),
      CoHandler(ConverterKey(asString,asDBObj))(stringToObj),
      CoHandler(ConverterKey(asUUID,asString))(_⇒"...")
    )
  private def stringToObj(value: String) = if(value.isEmpty) findNodes.noNode else Never()
  private def objIdToUIString(value: ObjId) = objToUIString(findNodes.whereObjId(value))
  private def objToUIString(obj: Obj): String = {
    val res = obj(at.caption)
    if(res.nonEmpty) {return res }
    val objId = obj(nodeAttrs.objId)
    handlerLists.single(ObjIdCaption(objId), ()⇒
      //s"${objIdFactory.toUUIDString(objId).substring(0,9)}..."
      "Unnamed Entity"
    )
  }
  def captions(label: Attr[Obj], attributes: List[Attr[_]])(calculate: Obj⇒String) =
    onUpdate.handlers(List(label), attributes)( (on,obj)⇒
      obj(at.caption) = if(!on) "" else {
        val text = calculate(obj)
        if(text.nonEmpty) text else s"Unnamed ${caption(label)}"
      }
    )

  private def caption(attr: Attr[_]) =
    handlerLists.single(AttrCaption(attr), ()⇒attrFactory.attrId(attr).toString)
}
