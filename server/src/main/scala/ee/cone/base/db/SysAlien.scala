package ee.cone.base.db

import java.util.UUID

import ee.cone.base.connection_api._
import ee.cone.base.util.Single

class AlienAccessAttrs(
  attr: AttrFactory,
  searchIndex: SearchIndex,
  nodeValueConverter: RawValueConverter[Obj],
  uuidValueConverter: RawValueConverter[Option[UUID]],
  stringValueConverter: RawValueConverter[String],
  mandatory: Mandatory
)(
  val targetSrcId: Attr[Option[UUID]] = attr(new PropId(0x0022), uuidValueConverter),
  val targetStringValue: Attr[String] = attr(new PropId(0x0023), stringValueConverter)
)()

class AlienCanChange(
  at: AlienAccessAttrs, handlerLists: CoHandlerLists,
  allNodes: DBNodes, mainTx: CurrentTx[MainEnvKey]
) {
  def apply(attr: Attr[String]) = handlers(at.targetStringValue)(attr)
  def handlers[Value](targetAttr: Attr[Value])(attr: Attr[Value]) =
    CoHandler(ChangeEventAdder(attr)){ node =>
      val srcId = node(allNodes.srcId)
      val addEvent = handlerLists.single(AddEvent)
      newValue => addEvent{ event =>
        event(at.targetSrcId) = srcId
        event(targetAttr) = newValue
        attr.defined
      }
    } ::
      CoHandler(ApplyEvent(attr.defined)){ event =>
        val node = allNodes.whereSrcId(mainTx(), event(at.targetSrcId).get)
        node(attr) = event(targetAttr)
      } :: Nil
}
