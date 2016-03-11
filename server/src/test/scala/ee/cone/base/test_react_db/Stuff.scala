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
  label: LabelFactory,
  searchIndex: SearchIndex,
  definedValueConverter: RawValueConverter[Unit],
  nodeValueConverter: RawValueConverter[Obj],
  uuidValueConverter: RawValueConverter[Option[UUID]],
  stringValueConverter: RawValueConverter[String],
  mandatory: Mandatory,
  alienCanChange: AlienCanChange
)(
  val asTestTask: Attr[Obj] = label(0x6600),
  val testState: Attr[String] = attr(new PropId(0x6601), stringValueConverter),
  val comments: Attr[String] = attr(new PropId(0x6602), stringValueConverter),
  val createTaskEvent: Attr[Boolean] = attr(new PropId(0x6603), definedValueConverter),
  val removeTaskEvent: Attr[Boolean] = attr(new PropId(0x6604), definedValueConverter)
)(val handlers: List[BaseCoHandler] =
  mandatory(asTestTask,testState, mutual = true) :::
  mandatory(asTestTask,comments, mutual = true) :::
  searchIndex.handlers(asTestTask.defined, testState) :::
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
  private def emptyView(pf: String) = tags.root(tags.text("text","Loading...")::Nil)
  private def testView(pf: String) = eventSourceOperations.incrementalApplyAndView { () ⇒
    val commentsRef = alienAttr(at.comments,"comments")
    val removeRef =
    val spans = allNodes.where(mainTx(), at.asTestTask.defined, at.testState, "A", Nil).map{
      task =>
        tags.span(
          task(allNodes.srcId).toString,
          tags.input(task(commentsRef)) ::
            tags.button("remove", "-", removeRef(task)) ::
            Nil
        )
    }
    tags.root(spans)
  }

  private def applyCreateTask(ev: Obj): Unit = {
    ev(at.target)
  }

  def handlers =
    CoHandler(ApplyEvent(at.createTaskEvent))(applyCreateTask) ::
    CoHandler(ViewPath(""))(emptyView) ::
    CoHandler(ViewPath("/test"))(testView) :: Nil
}


