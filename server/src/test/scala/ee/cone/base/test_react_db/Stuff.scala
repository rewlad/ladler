package ee.cone.base.test_react_db

import java.util.UUID

import ee.cone.base.connection_api._
import ee.cone.base.util.{Never, Single}
import ee.cone.base.vdom.Types.VDomKey
import ee.cone.base.vdom.{CurrentVDom, ViewPath}
import ee.cone.base.db._

class FailOfConnection(
  sender: SenderOfConnection
) extends CoHandlerProvider {
  def handlers = CoHandler(FailEventKey){ e =>
    println(e.toString)
    sender.sendToAlien("fail",e.toString) //todo
  } :: Nil
}

/*
class DynEdit(
  eventSourceOperations: SessionEventSourceOperations
) extends CoHandlerProvider {
  //lazy val
  def handlers = CoHandler(ViewPath("/db")){ pf =>
    // until = ???
    eventSourceOperations.incrementalApplyAndView{ ()⇒
      ???
    }
  } :: Nil
}
*/

class TestAttrs(
  attr: AttrFactory,
  searchIndex: SearchIndex,
  nodeValueConverter: RawValueConverter[DBNode],
  uuidValueConverter: RawValueConverter[UUID],
  stringValueConverter: RawValueConverter[String],
  mandatory: Mandatory,
  alienCanChange: AlienCanChange
)(
  val asTestTask: Attr[DBNode] = attr(0x6600, 0, nodeValueConverter),
  val testState: Attr[String] = attr(0, 0x6601, stringValueConverter),
  val comments: Attr[String] = attr(0, 0x6602, stringValueConverter)
)(val handlers: List[BaseCoHandler] =
  mandatory(asTestTask,testState, mutual = true) :::
  mandatory(asTestTask,comments, mutual = true) :::
  alienCanChange(comments) ::: Nil
) extends CoHandlerProvider

class TestView(
  at: TestAttrs,
  alienAccessAttrs: AlienAccessAttrs,
  handlerLists: CoHandlerLists,
  allNodes: DBNodes, mainTx: CurrentTx[MainEnvKey],
  eventSourceOperations: SessionEventSourceOperations,
  tags: Tags,
  alienAttr: AlienAttrFactory
) extends CoHandlerProvider {
  def handlers =
    CoHandler(ViewPath("/test")) { pf =>
      eventSourceOperations.incrementalApplyAndView { () ⇒
        val commentsRef = alienAttr(at.comments,"comments")
        val spans = allNodes.where(mainTx(), at.asTestTask.defined, at.testState, "A").map(
          task =>
            tags.span(task(alienAccessAttrs.srcId).toString,
              tags.input(task(commentsRef)) ::
                // btn
                Nil
            )
        )
        tags.root(spans)
      }
    } :: Nil
}

// to vDom?
case class AlienRef[Value](key: VDomKey, value: Value)(val onChange: Value=>Unit)
class AlienAttrFactory(handlerLists: CoHandlerLists, currentVDom: CurrentVDom) {
  def apply[Value](attr: Attr[Value], key: VDomKey) = { //when handlers are are available
    val eventAdder = Single(handlerLists.list(ChangeEventAdder(attr)))
    new Attr[AlienRef[Value]] {
      def defined = Never()
      def get(node: DBNode) = { // when making input tag
        val addEvent = eventAdder(node) // remembers srcId
        AlienRef(key, node(attr)){ newValue =>
          addEvent(newValue)
          currentVDom.invalidate()
        }
      }
      def set(node: DBNode, value: AlienRef[Value]) = Never()
    }
  }
}

// to db
class AlienAccessAttrs(
  attr: AttrFactory,
  searchIndex: SearchIndex,
  nodeValueConverter: RawValueConverter[DBNode],
  uuidValueConverter: RawValueConverter[UUID],
  stringValueConverter: RawValueConverter[String],
  mandatory: Mandatory
)(
  val asSrcIdentifiable: Attr[DBNode] = attr(0x0020, 0, nodeValueConverter),
  val srcId: Attr[UUID] = attr(0, 0x0021, uuidValueConverter),
  val targetSrcId: Attr[UUID] = attr(0, 0x0022, uuidValueConverter),
  val targetStringValue: Attr[String] = attr(0, 0x0023, stringValueConverter)
)(val handlers: List[BaseCoHandler] =
  mandatory(asSrcIdentifiable, srcId, mutual = true) :::
  searchIndex.handlers(asSrcIdentifiable, srcId) :::
  Nil
)
class AlienCanChange(
  at: AlienAccessAttrs, eventSourceOperations: SessionEventSourceOperations,
  allNodes: DBNodes, mainTx: CurrentTx[MainEnvKey]
) {
  def apply(attr: Attr[String]) = handlers(at.targetStringValue)(attr)
  def handlers[Value](targetAttr: Attr[Value])(attr: Attr[Value]) =
    CoHandler(ChangeEventAdder(attr)){ node =>
      val srcId = node(at.srcId)
      newValue => eventSourceOperations.addEvent(attr.defined){ event =>
        event(at.targetSrcId) = srcId
        event(targetAttr) = newValue
      }
    } ::
    CoHandler(ApplyEvent(attr.defined)){ event =>
      val node = Single(allNodes.where(
        mainTx(), at.asSrcIdentifiable.defined, at.srcId, event(at.targetSrcId)
      ))
      node(attr) = event(targetAttr)
    } :: Nil
}

//to api
case class ChangeEventAdder[Value](attr: Attr[Value]) extends EventKey[DBNode,Value=>Unit]
