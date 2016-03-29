package ee.cone.base.db

import java.util.UUID

import ee.cone.base.connection_api._
import ee.cone.base.util.Single

class AlienAccessAttrs(
  attr: AttrFactory,
  searchIndex: SearchIndex,
  nodeValueConverter: RawValueConverter[Obj],
  uuidValueConverter: RawValueConverter[Option[UUID]],
  mandatory: Mandatory
)(
  val targetSrcId: Attr[Option[UUID]] = attr(new PropId(0x0022), uuidValueConverter)
)()

class AlienCanChange(
  at: AlienAccessAttrs, handlerLists: CoHandlerLists,
  uniqueNodes: UniqueNodes, mainTx: CurrentTx[MainEnvKey]
) {
  private def eventSource = handlerLists.single(SessionEventSource)
  def handlers[Value](targetAttr: Attr[Value])(attr: Attr[Value]) =
      CoHandler(AddChangeEvent(attr)){ (srcId:UUID,newValue:Value) =>
        eventSource.addEvent{ event =>
          event(at.targetSrcId) = Option(srcId)
          event(targetAttr) = newValue
          (attr.defined, s"value of $attr was changed to $newValue")
        }
      } ::
      CoHandler(ApplyEvent(attr.defined)){ event =>
        //println(event(at.targetSrcId).get)
        val node = uniqueNodes.whereSrcId(mainTx(), event(at.targetSrcId).get)
        node(attr) = event(targetAttr)
      } :: Nil
}
