package ee.cone.base.test_react_db

import java.util.UUID

import ee.cone.base.connection_api._
import ee.cone.base.server.SenderOfConnection
import ee.cone.base.util.{Never, Single}
import ee.cone.base.vdom.Types.VDomKey
import ee.cone.base.vdom.{Tags, CurrentVDom, ViewPath}
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
  asDefined: AttrValueType[Boolean],
  asObj: AttrValueType[Obj],
  asUUID: AttrValueType[Option[UUID]],
  asString: AttrValueType[String]
)(
  val asTestTask: Attr[Obj] = label("690cb4c2-55e8-4fca-bf23-394fbb2c65ba"),
  val testState: Attr[String] = attr("6e60c1f1-a0b2-4a9a-84f7-c3627ac50727", asString),
  val comments: Attr[String] = attr("c9ab1b7a-5339-4360-aa8d-b3c47d0099cf", asString),
  val taskCreated: Attr[Boolean] = attr("8af608d3-7c5d-42dc-be26-c4aa1a073638", asDefined),
  val taskRemoved: Attr[Boolean] = attr("9e86aae3-2094-4b38-a38b-41c1e285410d", asDefined)
)

class TestComponent(
  at: TestAttrs,
  alienAccessAttrs: AlienAccessAttrs,
  handlerLists: CoHandlerLists,
  findNodes: FindNodes,
  mainTx: CurrentTx[MainEnvKey],
  rTags: Tags,
  tags: TestTags,
  currentVDom: CurrentVDom,
  searchIndex: SearchIndex,
  mandatory: Mandatory,
  alienCanChange: Alien,
  factIndex: FactIndex
) extends CoHandlerProvider {
  import rTags._
  private def eventSource = handlerLists.single(SessionEventSource, ()⇒Never())
  private def emptyView(pf: String) =
    root(List(text("text", "Loading...")))
  private def testView(pf: String) = {
    eventSource.incrementalApplyAndView { () ⇒
      val startTime = System.currentTimeMillis
      val tasks = findNodes.where(
        mainTx(),
        at.asTestTask,
        at.testState,
        "A",
        Nil
      )
      val taskLines = tasks.map { obj =>
        val task = alienCanChange.wrap(obj)
        val objIdStr = task(alienCanChange.objIdStr)
        tags.div(
          objIdStr,
          tags.input("comments", task(at.comments), task(at.comments)=_) ::
            tags.button("remove", "-", removeTaskAction(task)) ::
            //tags.text("dbg0",if(task(at.asTestTask.defined)) "D" else "d") ::
            //tags.text("dbg1",s"[${task(at.testState)}]") ::
            Nil
        )
      }
      val eventLines = eventSource.unmergedEvents.map { ev =>
        val objIdStr = ev(alienCanChange.objIdStr)
        tags.div(
          objIdStr,
          text("text", ev(eventSource.comment)) ::
          tags.button("remove", "-", ()=>eventSource.addUndo(ev)) ::
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
  private def dumpAction()() = handlerLists.single(DumpKey, ()⇒Never())(mainTx)

  private def saveAction()() = {
    eventSource.addRequest()
    currentVDom.invalidate()
  }
  private def removeTaskAction(obj: Obj)() = {
    eventSource.addEvent{ ev =>
      ev(alienAccessAttrs.targetObj) = obj
      (at.taskRemoved, "task was removed")
    }
    currentVDom.invalidate()
  }
  private def taskRemoved(ev: Obj): Unit = {
    val task = ev(alienAccessAttrs.targetObj)
    task(at.asTestTask) = findNodes.noNode
    task(at.comments) = ""
    task(at.testState) = ""
  }
  private def createTaskAction()() = {
    eventSource.addEvent{ ev =>
      ev(alienAccessAttrs.targetObj) = findNodes.whereObjId(findNodes.toObjId(UUID.randomUUID))
      (at.taskCreated, "task was created")
    }
    currentVDom.invalidate()
  }
  private def taskCreated(ev: Obj): Unit = {
    val task = ev(alienAccessAttrs.targetObj)
    task(at.asTestTask) = task
    task(at.testState) = "A"
  }


  def handlers =
    mandatory(at.asTestTask,at.testState, mutual = true) :::
    mandatory(at.asTestTask,at.comments, mutual = true) :::
    searchIndex.handlers(at.asTestTask, at.testState) :::
    factIndex.handlers(at.testState) :::
    alienCanChange.update(at.comments) :::
    CoHandler(ApplyEvent(at.taskCreated))(taskCreated) ::
    CoHandler(ApplyEvent(at.taskRemoved))(taskRemoved) ::
    CoHandler(ViewPath(""))(emptyView) ::
    CoHandler(ViewPath("/test"))(testView) :: Nil
}

