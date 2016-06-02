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
  val objIdStr: Attr[String] = attr("4a7ebc6b-e3db-4d7a-ae10-eab15370d690", asString),
  val isEditing: Attr[Boolean] = attr("3e7fbcd6-4707-407f-911e-7493b017afc1",asBoolean)
)

class DemandedNode(var objId: ObjId, val setup: Obj⇒Unit)

class AlienWrapType extends WrapType[Unit]
class DemandedWrapType extends WrapType[DemandedNode]

case class AttrCaption(attr: Attr[_]) extends EventKey[String]

class Alien(
  at: AlienAccessAttrs, nodeAttrs: NodeAttrs, attrFactory: AttrFactory,
  handlerLists: CoHandlerLists,
  findNodes: FindNodes, mainTx: CurrentTx[MainEnvKey], factIndex: FactIndex,
  alienWrapType: WrapType[Unit], demandedWrapType: WrapType[DemandedNode], dbWrapType: DBWrapType,
  objIdFactory: ObjIdFactory
) extends CoHandlerProvider {
  private def eventSource = handlerLists.single(SessionEventSource, ()⇒Never())
  def caption(attr: Attr[_]) =
    handlerLists.single(AttrCaption(attr), ()⇒attrFactory.attrId(attr).toString)
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
          (attrId, s"'${caption(attr)}' of ... was changed to '$newValue'")
        }
    } ::
    CoHandler(ApplyEvent(attrId)){ event =>
      event(at.targetObj)(attr) = event(targetAttr)
    } :: Nil
  }
  def wrapForEdit(obj: Obj): Obj = obj.wrap(alienWrapType, ())
  def demandedNode(setup: Obj⇒Unit): Obj = {
    wrapForEdit(findNodes.noNode).wrap(demandedWrapType, new DemandedNode(objIdFactory.noObjId,setup))
  }
  def objIdStr: Attr[String] = at.objIdStr
  def handlers = List(
    CoHandler(GetValue(demandedWrapType,nodeAttrs.objId)){ (obj,innerObj)⇒
      innerObj.data.objId
    },
    CoHandler(GetValue(dbWrapType, objIdStr)){ (obj, innerObj)⇒
      objIdFactory.toUUIDString(obj(nodeAttrs.objId))
    },
    CoHandler(GetValue(dbWrapType, at.isEditing)){ (obj, innerObj)⇒ false },
    CoHandler(GetValue(alienWrapType, at.isEditing)){ (obj, innerObj)⇒ true }
  ) ::: factIndex.handlers(at.targetObj)
}
