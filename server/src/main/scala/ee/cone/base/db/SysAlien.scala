package ee.cone.base.db

import java.util.UUID

import ee.cone.base.connection_api._
import ee.cone.base.util.{Never, Single}

class AlienAccessAttrs(
  objIdFactory: ObjIdFactory,
  attr: AttrFactory,
  asNode: AttrValueType[Obj],
  asString: AttrValueType[String],
  asBoolean: AttrValueType[Boolean]
)(
  val target: ObjId = objIdFactory.toObjId("5a7300e9-a1d9-41a6-959f-cbd2f6791deb"),
  val created: ObjId = objIdFactory.toObjId("7947ca07-d72f-438d-9e21-1ed8196689ae"),
  val targetObj: Attr[Obj] = attr("0cd4abcb-c83f-4e15-aa9f-d217f2e36596", asNode),
  val isEditing: Attr[Boolean] = attr("3e7fbcd6-4707-407f-911e-7493b017afc1",asBoolean),
  val comment: Attr[String] = attr("c0e6114b-bfb2-49fc-b9ef-5110ed3a9521", asString)
)

class DemandedNode(var objId: ObjId, val setup: Obj⇒Unit)

class AlienWrapType extends WrapType[Unit]
class DemandedWrapType extends WrapType[DemandedNode]

class Alien(
  at: AlienAccessAttrs, nodeAttrs: NodeAttrs, uiStringAttributes: UIStringAttributes,
  attrFactory: AttrFactory,
  handlerLists: CoHandlerLists,
  findNodes: FindNodes, mainTx: CurrentTx[MainEnvKey], factIndex: FactIndex,
  alienWrapType: WrapType[Unit], demandedWrapType: WrapType[DemandedNode], dbWrapType: DBWrapType,
  objIdFactory: ObjIdFactory,
  uiStrings: UIStrings,
  asDBObj: AttrValueType[Obj],
  asString:   AttrValueType[String],
  transient: Transient
) extends CoHandlerProvider {
  private def eventSource = handlerLists.single(SessionEventSource, ()⇒Never())
  def update[Value](attr: Attr[Value]) = {
    val attrId = attrFactory.attrId(attr)
    val targetAttr = attrFactory.define(objIdFactory.compose(List(at.target, attrId)), attrFactory.valueType(attr))
    factIndex.handlers(targetAttr) :::
    CoHandler(SetValue(demandedWrapType, attr)){
      (obj: Obj, innerObj : InnerObj[DemandedNode], value: Value)⇒
      val demanded = innerObj.data
      if(!demanded.objId.nonEmpty){
        demanded.objId = findNodes.toObjId(UUID.randomUUID)
        demanded.setup(obj)
      }
      innerObj.next.set(obj, attr, value)
    } ::
    CoHandler(SetValue(alienWrapType, attr)){
      (obj: Obj, innerObj : InnerObj[Unit], newValue: Value)⇒
        eventSource.addEvent{ event =>
          event(at.targetObj) = obj
          event(targetAttr) = newValue
          attrId
        }
    } ::
    CoHandler(ApplyEvent(attrId)){ event =>
      val obj = event(at.targetObj)
      val value = event(targetAttr)
      val attrStr = uiStrings.caption(attr)
      val objStr = uiStrings.converter(asDBObj,asString)(obj)
      val valStr = uiStrings.converter(attrFactory.valueType(attr),asString)(value)
      obj(attr) = value
      describeEvent(event, s"'$attrStr' of '$objStr' was changed to '$valStr'")
    } :: Nil
  }
  def describeEvent(event: Obj, comment: String) = event(at.comment) = comment //todo
  def wrapForEdit(obj: Obj): Obj = obj.wrap(alienWrapType, ())
  def demandedNode(setup: Obj⇒Unit): Obj = {
    wrapForEdit(findNodes.noNode).wrap(demandedWrapType, new DemandedNode(objIdFactory.noObjId,setup))
  }
  def objIdStr: Attr[String] = uiStringAttributes.objIdStr
  def handlers = List(
    CoHandler(GetValue(demandedWrapType,nodeAttrs.objId)){ (obj,innerObj)⇒
      innerObj.data.objId
    },
    CoHandler(GetValue(dbWrapType, objIdStr)){ (obj, innerObj)⇒
      objIdFactory.toUUIDString(obj(nodeAttrs.objId))
    },
    CoHandler(GetValue(dbWrapType, at.isEditing)){ (obj, innerObj)⇒ false },
    CoHandler(GetValue(alienWrapType, at.isEditing)){ (obj, innerObj)⇒ true }
  ) ::: factIndex.handlers(at.targetObj) ::: transient.update(at.comment)
}
