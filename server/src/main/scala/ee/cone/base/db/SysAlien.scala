package ee.cone.base.db

import java.util.UUID

import ee.cone.base.connection_api._
import ee.cone.base.util.{Never, Single}

class AlienAccessAttrs(
  attr: AttrFactory,
  asDefined: AttrValueType[Boolean],
  asNode: AttrValueType[Obj]
)(
  val target: Attr[Boolean] = attr("5a7300e9-a1d9-41a6-959f-cbd2f6791deb", asDefined),
  val created: Attr[Boolean] = attr("7947ca07-d72f-438d-9e21-1ed8196689ae", asDefined),
  val targetObj: Attr[Obj] = attr("0cd4abcb-c83f-4e15-aa9f-d217f2e36596", asNode)
)

class DemandedNode(var objId: ObjId)(val setup: Obj⇒Unit)

class AlienCanChange(
  at: AlienAccessAttrs, nodeAttrs: NodeAttrs, attrFactory: AttrFactory,
  nodeFactory: NodeFactory, handlerLists: CoHandlerLists,
  uniqueNodes: UniqueNodes, mainTx: CurrentTx[MainEnvKey], factIndex: FactIndex,
  alienWrapType: WrapType[DemandedNode]
) extends CoHandlerProvider {
  private def eventSource = handlerLists.single(SessionEventSource, ()⇒Never())
  def update[Value](attr: Attr[Value]) = {
    val definedAttr = attrFactory.defined(attr)
    val targetAttr = attrFactory.derive(at.target, attr)
    factIndex.handlers(attr) ::: factIndex.handlers(targetAttr) :::
    CoHandler(SetValue(alienWrapType, attr)){
      (obj: Obj, innerObj : InnerObj[DemandedNode], newValue: Value)⇒
        val demanded = innerObj.data
        if(!demanded.objId.nonEmpty){
          val newObj = uniqueNodes.whereObjId(UUID.randomUUID)
          demanded.objId = newObj(nodeAttrs.objId)
          demanded.setup(obj)
        }
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
  def wrap(obj: Obj)(setup: Obj⇒Unit = _⇒Never()): Obj = {
    obj.wrap(alienWrapType, new DemandedNode(obj(nodeAttrs.objId))(setup))
  }
  def handlers =
    CoHandler(GetValue(alienWrapType,nodeAttrs.objId)){ (obj,innerObj)⇒
      innerObj.data.objId
    } ::
    factIndex.handlers(at.targetObj)
}
