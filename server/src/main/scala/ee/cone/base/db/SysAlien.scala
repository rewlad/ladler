package ee.cone.base.db

import java.util.UUID

import ee.cone.base.connection_api._
import ee.cone.base.util.{Never, Single}

class AlienAccessAttrs(
  attr: AttrFactory,
  asDefined: AttrValueType[Boolean]
)(
  val target: Attr[Boolean] = attr("5a7300e9-a1d9-41a6-959f-cbd2f6791deb", asDefined),
  val created: Attr[Boolean] = attr("7947ca07-d72f-438d-9e21-1ed8196689ae", asDefined)
)

class DemandedNode(var srcId: Option[UUID])(val setup: (Obj,UUID)⇒Unit)

class AlienCanChange(
  at: AlienAccessAttrs, nodeAttrs: NodeAttrs, attrFactory: AttrFactory, handlerLists: CoHandlerLists,
  uniqueNodes: UniqueNodes, mainTx: CurrentTx[MainEnvKey], factIndex: FactIndex,
  alienWrapType: WrapType[DemandedNode]
)(
  val targetSrcId: Attr[Option[UUID]] = attrFactory.derive(at.target, uniqueNodes.srcId)
) extends CoHandlerProvider {
  private def eventSource = handlerLists.single(SessionEventSource, ()⇒Never())
  def update[Value](attr: Attr[Value]) = {
    val definedAttr = attrFactory.defined(attr)
    val targetAttr = attrFactory.derive(at.target, attr)
    factIndex.handlers(attr) ::: factIndex.handlers(targetAttr) :::
    CoHandler(SetValue(alienWrapType, attr)){
      (obj: Obj, innerObj : InnerObj[DemandedNode], newValue: Value)⇒
        val demanded = innerObj.data
        if(demanded.srcId.isEmpty){
          demanded.srcId = Option(UUID.randomUUID)
          demanded.setup(obj, innerObj.data.srcId.get)
        }
        eventSource.addEvent{ event =>
          event(targetSrcId) = demanded.srcId
          event(targetAttr) = newValue
          (definedAttr, s"value of $attr was changed to $newValue")
        }
    } ::
    CoHandler(ApplyEvent(definedAttr)){ event =>
      //println(event(at.targetSrcId).get)
      val node = uniqueNodes.whereSrcId(mainTx(), event(targetSrcId).get)
      node(attr) = event(targetAttr)
    } :: Nil
  }
  def create(labelAttr: Attr[Obj]) = {
    val labelDefinedAttr = attrFactory.defined(labelAttr)
    val eventAttr = attrFactory.derive(at.created, labelDefinedAttr)
    factIndex.handlers(labelAttr) :::
    CoHandler(AddCreateEvent(labelDefinedAttr)) { srcId ⇒
      eventSource.addEvent { ev =>
        ev(targetSrcId) = Option(srcId)
        (eventAttr, s"$labelAttr was created")
      }
    } ::
    CoHandler(ApplyEvent(eventAttr)) { ev =>
      val srcId = ev(targetSrcId).get
      val item = uniqueNodes.create(mainTx(), labelAttr, srcId)
    } :: Nil
  }
  def wrap(obj: Obj)(setup: (Obj,UUID)⇒Unit = (_,_)⇒Never()): Obj = {
    var srcId = if(obj(nodeAttrs.nonEmpty)) Some(obj(uniqueNodes.srcId).get) else None
    obj.wrap(alienWrapType, new DemandedNode(srcId)(setup))
  }
  def handlers =
    CoHandler(GetValue(alienWrapType,uniqueNodes.srcId)){ (obj,innerObj)⇒
      innerObj.data.srcId
    } ::
    factIndex.handlers(targetSrcId)
}
