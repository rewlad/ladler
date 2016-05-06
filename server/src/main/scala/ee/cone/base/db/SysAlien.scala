package ee.cone.base.db

import java.util.UUID

import ee.cone.base.connection_api._
import ee.cone.base.util.Single

class AlienAccessAttrs(
  attr: AttrFactory,
  definedValueConverter: RawValueConverter[Boolean]
)(
  val target: Attr[Boolean] = attr("5a7300e9-a1d9-41a6-959f-cbd2f6791deb", definedValueConverter),
  val created: Attr[Boolean] = attr("7947ca07-d72f-438d-9e21-1ed8196689ae", definedValueConverter)
)()

class AlienCanChange(
  at: AlienAccessAttrs, attrFactory: AttrFactory, handlerLists: CoHandlerLists,
  uniqueNodes: UniqueNodes, mainTx: CurrentTx[MainEnvKey]
) {
  private def eventSource = handlerLists.single(SessionEventSource)
  private def targetSrcId = attrFactory.derive(at.target, uniqueNodes.srcId)
  def update[Value](attr: Attr[Value]) = {
    val definedAttr = attrFactory.defined(attr)
    val targetAttr = attrFactory.derive(at.target, attr)
    CoHandler(AddUpdateEvent(attr)){ (srcId:UUID,newValue:Value) =>
      eventSource.addEvent{ event =>
        event(targetSrcId) = Option(srcId)
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
}
