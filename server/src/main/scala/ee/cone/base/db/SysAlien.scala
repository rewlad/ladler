package ee.cone.base.db

import java.util.UUID

import ee.cone.base.connection_api._
import ee.cone.base.util.{Never, Single}

class AlienAttributesImpl(
  objIdFactory: ObjIdFactory,
  attr: AttrFactory,
  asNode: AttrValueType[Obj],
  asString: AttrValueType[String],
  asBoolean: AttrValueType[Boolean]
)(
  val objIdStr: Attr[String] = attr("4a7ebc6b-e3db-4d7a-ae10-eab15370d690", asString),//f
  val target: ObjId = objIdFactory.toObjId("5a7300e9-a1d9-41a6-959f-cbd2f6791deb"),
  val targetObj: Attr[Obj] = attr("0cd4abcb-c83f-4e15-aa9f-d217f2e36596", asNode),
  val isEditing: Attr[Boolean] = attr("3e7fbcd6-4707-407f-911e-7493b017afc1",asBoolean),//f
  val comment: Attr[String] = attr("c0e6114b-bfb2-49fc-b9ef-5110ed3a9521", asString)
) extends AlienAttributes

class DemandedNode(var objId: ObjId, val setup: Obj⇒Unit)

class AlienWrapType extends WrapType[Unit]
class DemandedWrapType extends WrapType[DemandedNode]

class AlienImpl(
  at: AlienAttributesImpl, nodeAttrs: NodeAttrs,
  attrFactory: AttrFactory,
  handlerLists: CoHandlerLists,
  findNodes: FindNodes, mainTx: CurrentTx[MainEnvKey], factIndex: FactIndex,
  alienWrapType: WrapType[Unit], demandedWrapType: WrapType[DemandedNode],
  objIdFactory: ObjIdFactory,
  uiStrings: UIStrings,
  asDBObj: AttrValueType[Obj],
  asString:   AttrValueType[String],
  transient: Transient
) extends Alien with CoHandlerProvider {
  private def eventSource = handlerLists.single(SessionEventSource, ()⇒Never())
  def update[Value](attr: Attr[Value]) = {
    val attrId = attrFactory.attrId(attr)
    val targetAttr = attrFactory.define(objIdFactory.compose(List(at.target, attrId)), attrFactory.valueType(attr))
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
      val description = if(objStr == valStr) s"'$objStr' became '$attrStr'"
        else s"'$attrStr' of '$objStr' was changed to '$valStr'"
      event(at.comment) = description
    } :: Nil
  }
  def wrapForUpdate(obj: Obj): Obj = obj.wrap(alienWrapType, ())
  def demanded(setup: Obj⇒Unit): Obj = {
    wrapForUpdate(findNodes.noNode).wrap(demandedWrapType, new DemandedNode(objIdFactory.noObjId,setup))
  }
  def handlers =
    List(
      CoHandler(GetValue(demandedWrapType,nodeAttrs.objId)){ (obj,innerObj)⇒
        innerObj.data.objId
      },
      CoHandler(GetValue(alienWrapType, at.isEditing)){ (obj, innerObj)⇒ true }
    ) :::
    attrFactory.handlers(at.objIdStr) { (obj, objId) ⇒
      val theObjId = obj(nodeAttrs.objId)
      if(theObjId.nonEmpty) objIdFactory.toUUIDString(theObjId) else "none"
    } :::
    attrFactory.handlers(at.isEditing)( (obj, objId) ⇒ false ) :::
    factIndex.handlers(at.targetObj) ::: transient.update(at.comment)
}
