package ee.cone.base.test_react_db

import java.util.UUID

import ee.cone.base.connection_api._
import ee.cone.base.server.SenderOfConnection
import ee.cone.base.util.{Never, Single}
import ee.cone.base.vdom.Types.VDomKey
import ee.cone.base.vdom.{Tags, AlienAttrFactory, CurrentVDom, ViewPath}
import ee.cone.base.db._

class FailOfConnection(
  sender: SenderOfConnection
) extends CoHandlerProvider {
  def handlers = CoHandler(FailEventKey){ e =>
    println(s"error: ${e.toString}")
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
  val asTestTask: Attr[Obj] = label("690cb4c2-55e8-4fca-bf23-394fbb2c65ba"),
  val testState: Attr[String] = attr("6e60c1f1-a0b2-4a9a-84f7-c3627ac50727", stringValueConverter),
  val comments: Attr[String] = attr("c9ab1b7a-5339-4360-aa8d-b3c47d0099cf", stringValueConverter),
  val taskCreated: Attr[Boolean] = attr("8af608d3-7c5d-42dc-be26-c4aa1a073638", definedValueConverter),
  val taskRemoved: Attr[Boolean] = attr("9e86aae3-2094-4b38-a38b-41c1e285410d", definedValueConverter)
)(val handlers: List[BaseCoHandler] =
  mandatory(asTestTask,testState, mutual = true) :::
  mandatory(asTestTask,comments, mutual = true) :::
  searchIndex.handlers(asTestTask, testState) :::
  alienCanChange.update(comments) ::: Nil
) extends CoHandlerProvider

class TestComponent(
  at: TestAttrs,
  alienAccessAttrs: AlienAccessAttrs,
  handlerLists: CoHandlerLists,
  findNodes: FindNodes, uniqueNodes: UniqueNodes, mainTx: CurrentTx[MainEnvKey],
  rTags: Tags,
  tags: TestTags,
  alienAttr: AlienAttrFactory,
  currentVDom: CurrentVDom
) extends CoHandlerProvider {
  import rTags._
  private def eventSource = handlerLists.single(SessionEventSource)
  private def emptyView(pf: String) =
    root(List(text("text", "Loading...")))
  private def testView(pf: String) = {
    eventSource.incrementalApplyAndView { () ⇒
      val startTime = System.currentTimeMillis
      val changeComments: (UUID) => (String) => Unit = alienAttr(at.comments)
      val tasks = findNodes.where(
        mainTx(),
        at.asTestTask,
        at.testState,
        "A",
        Nil
      )
      val taskLines = tasks.map { task =>
        val srcId = task(uniqueNodes.srcId).get
        tags.div(
          srcId.toString,
          tags.input("comments", task(at.comments), changeComments(srcId)) ::
            tags.button("remove", "-", removeTaskAction(srcId)) ::
            //tags.text("dbg0",if(task(at.asTestTask.defined)) "D" else "d") ::
            //tags.text("dbg1",s"[${task(at.testState)}]") ::
            Nil
        )
      }
      val eventLines = eventSource.unmergedEvents.map { ev =>
        val srcId = ev(uniqueNodes.srcId).get
        tags.div(
          srcId.toString,
          text("text", ev(eventSource.comment)) ::
          tags.button("remove", "-", ()=>eventSource.addUndo(srcId)) ::
          Nil
        )
      }
      val btnList = List(
        tags.button("save", "save", saveAction()),
        tags.button("add", "+", createTaskAction()),
        tags.button("fail", "fail", failAction()),
        tags.button("dump", "dump", dumpAction())
      )
      val res = root(List(btnList,taskLines,eventLines).flatten)

      val endTime = System.currentTimeMillis
      currentVDom.until(endTime+(endTime-startTime)*10)
      res
    }
  }
  private def failAction()() = throw new Exception("test fail")
  private def dumpAction()() = handlerLists.single(DumpKey)(mainTx)

  private def saveAction()() = {
    eventSource.addRequest()
    currentVDom.invalidate()
  }
  private def removeTaskAction(srcId: UUID)() = {
    eventSource.addEvent{ ev =>
      ev(alienAccessAttrs.targetSrcId) = Option(srcId)
      (at.taskRemoved, "task was removed")
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
      (at.taskCreated, "task was created")
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


