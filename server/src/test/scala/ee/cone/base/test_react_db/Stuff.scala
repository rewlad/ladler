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
  definedValueConverter: RawValueConverter[Boolean],
  nodeValueConverter: RawValueConverter[Obj],
  uuidValueConverter: RawValueConverter[Option[UUID]],
  stringValueConverter: RawValueConverter[String],
  mandatory: Mandatory,
  alienCanChange: AlienCanChange
)(
  val asTestTask: Attr[Obj] = label(0x6600),
  val testState: Attr[String] = attr(new PropId(0x6601), stringValueConverter),
  val comments: Attr[String] = attr(new PropId(0x6602), stringValueConverter),
  val taskCreated: Attr[Boolean] = attr(new PropId(0x6603), definedValueConverter),
  val taskRemoved: Attr[Boolean] = attr(new PropId(0x6604), definedValueConverter)
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
  findNodes: FindNodes, uniqueNodes: UniqueNodes, mainTx: CurrentTx[MainEnvKey],
  tags: Tags,
  alienAttr: AlienAttrFactory,
  currentVDom: CurrentVDom
) extends CoHandlerProvider {
  private def eventSource = handlerLists.single(SessionEventSource)
  private def emptyView(pf: String) = tags
    .root(tags.text("text", "Loading...") :: Nil)
  private def testView(pf: String) =
    eventSource.incrementalApplyAndView { () ⇒
      val changeComments: (UUID) => (String) => Unit = alienAttr(at.comments)
      val tasks = findNodes.where(
        mainTx(),
        at.asTestTask.defined,
        at.testState,
        "A",
        Nil
      )
      val taskSpans = tasks.map { task =>
        val srcId = task(uniqueNodes.srcId).get
        tags.span(
          srcId.toString,
          tags.input("comments", task(at.comments), changeComments(srcId)) ::
            tags.button("remove", "-", removeTaskAction(srcId)) ::
            tags.text("dbg0",if(task(at.asTestTask.defined)) "D" else "d") ::
            tags.text("dbg1",s"[${task(at.testState)}]") ::
            Nil
        )
      }
      val saveBtn = tags.button("save", "save", saveAction())
      val addBtn = tags.button("add", "+", createTaskAction())
      tags.root(saveBtn :: addBtn :: taskSpans)
    }
  private def saveAction()() = {
    eventSource.addRequest()
    currentVDom.invalidate()
  }
  private def removeTaskAction(srcId: UUID)() = {
    eventSource.addEvent{ ev =>
      ev(alienAccessAttrs.targetSrcId) = Option(srcId)
      at.taskRemoved
    }
    currentVDom.invalidate()
  }
  private def taskRemoved(ev: Obj): Unit = {
    val srcId = ev(alienAccessAttrs.targetSrcId).get
    val task = uniqueNodes.whereSrcId(mainTx(), srcId)
    task(at.asTestTask) = uniqueNodes.noNode
    task(at.comments) = ""
    task(at.testState) = ""
  }
  private def createTaskAction()() = {
    eventSource.addEvent{ ev =>
      ev(alienAccessAttrs.targetSrcId) = Option(UUID.randomUUID)
      at.taskCreated
    }
    currentVDom.invalidate()
  }
  private def taskCreated(ev: Obj): Unit = {
    val srcId = ev(alienAccessAttrs.targetSrcId).get
    val task = uniqueNodes.create(mainTx(), at.asTestTask, srcId)
    task(at.testState) = "A"
  }


  def handlers =
    CoHandler(ApplyEvent(at.taskCreated))(taskCreated) ::
    CoHandler(ApplyEvent(at.taskRemoved))(taskRemoved) ::
    CoHandler(ViewPath(""))(emptyView) ::
    CoHandler(ViewPath("/test"))(testView) :: Nil
}


