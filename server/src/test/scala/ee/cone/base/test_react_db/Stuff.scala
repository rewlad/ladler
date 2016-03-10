package ee.cone.base.test_react_db

import java.util.UUID

import ee.cone.base.connection_api._
import ee.cone.base.server.SenderOfConnection
import ee.cone.base.util.{Never, Single}
import ee.cone.base.vdom.Types.VDomKey
import ee.cone.base.vdom.{AlienAttrFactory, CurrentVDom, ViewPath}
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
  nodeValueConverter: RawValueConverter[Obj],
  uuidValueConverter: RawValueConverter[Option[UUID]],
  stringValueConverter: RawValueConverter[String],
  mandatory: Mandatory,
  alienCanChange: AlienCanChange
)(
  val asTestTask: Attr[Obj] = attr(0x6600, 0, nodeValueConverter),
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
    CoHandler(ViewPath("")) { pf =>
      tags.root(tags.text("text","Loading...")::Nil)
    } ::
    CoHandler(ViewPath("/test")) { pf =>
      eventSourceOperations.incrementalApplyAndView { () ⇒
        val commentsRef = alienAttr(at.comments,"comments")
        val spans = allNodes.where(mainTx(), at.asTestTask.defined, at.testState, "A", Nil).map(
          task =>
            tags.span(task(allNodes.srcId).toString,
              tags.input(task(commentsRef)) ::
                // btn
                Nil
            )
        )
        tags.root(spans)
      }
    } :: Nil
}


