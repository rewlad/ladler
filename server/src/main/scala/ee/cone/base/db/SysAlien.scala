package ee.cone.base.db

import java.util.UUID

import ee.cone.base.connection_api._
import ee.cone.base.util.{Never, Single}

class AlienAccessAttrs(
  attr: AttrFactory,
  asDefined: AttrValueType[Boolean],
  asNode: AttrValueType[Obj],
  asString: AttrValueType[String]
)(
  val target: Attr[Boolean] = attr("5a7300e9-a1d9-41a6-959f-cbd2f6791deb", asDefined),
  val created: Attr[Boolean] = attr("7947ca07-d72f-438d-9e21-1ed8196689ae", asDefined),
  val targetObj: Attr[Obj] = attr("0cd4abcb-c83f-4e15-aa9f-d217f2e36596", asNode),
  val objIdStr: Attr[String] = attr("4a7ebc6b-e3db-4d7a-ae10-eab15370d690", asString)
)

class DemandedNode(var objId: ObjId, val setup: Obj⇒Unit)

class Alien(
  at: AlienAccessAttrs, nodeAttrs: NodeAttrs, attrFactory: AttrFactory,
  handlerLists: CoHandlerLists,
  findNodes: FindNodes, mainTx: CurrentTx[MainEnvKey], factIndex: FactIndex,
  alienWrapType: WrapType[Unit], demandedWrapType: WrapType[DemandedNode],
  noObjId: ObjId
) extends CoHandlerProvider {
  private def eventSource = handlerLists.single(SessionEventSource, ()⇒Never())
  def update[Value](attr: Attr[Value]) = {
    val definedAttr = attrFactory.defined(attr)
    val targetAttr = attrFactory.derive(at.target, attr)
    factIndex.handlers(attr) ::: factIndex.handlers(targetAttr) :::
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
          (definedAttr, s"value of $attr was changed to $newValue")
        }
    } ::
    CoHandler(ApplyEvent(definedAttr)){ event =>
      event(at.targetObj)(attr) = event(targetAttr)
    } :: Nil
  }
  def wrap(obj: Obj): Obj = obj.wrap(alienWrapType, ())
  def demandedNode(setup: Obj⇒Unit): Obj = {
    wrap(findNodes.noNode).wrap(demandedWrapType, new DemandedNode(noObjId,setup))
  }
  def objIdStr: Attr[String] = at.objIdStr
  def handlers = List(
    CoHandler(GetValue(demandedWrapType,nodeAttrs.objId)){ (obj,innerObj)⇒
      innerObj.data.objId
    },
    CoHandler(GetValue(alienWrapType, objIdStr)){ (obj, innerObj)⇒
      findNodes.toUUIDString(obj(nodeAttrs.objId))
    }) :::
    factIndex.handlers(at.targetObj)
}
