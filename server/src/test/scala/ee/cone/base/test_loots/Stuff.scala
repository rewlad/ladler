package ee.cone.base.test_loots

import ee.cone.base.connection_api.{CoHandlerLists, FailEventKey, CoHandler,
CoHandlerProvider}
import ee.cone.base.db.SessionEventSource
import ee.cone.base.server.SenderOfConnection
import ee.cone.base.vdom.{CurrentVDom, Tags, ViewPath}

class FailOfConnection(
  sender: SenderOfConnection
) extends CoHandlerProvider {
  def handlers = CoHandler(FailEventKey){ e =>
    println(e.toString)
    sender.sendToAlien("fail",e.toString) //todo
  } :: Nil
}

class TestComponent(
  handlerLists: CoHandlerLists,
  tags: Tags,
  currentVDom: CurrentVDom
) extends CoHandlerProvider {
  private def eventSource = handlerLists.single(SessionEventSource)

  private def emptyView(pf: String) =
    tags.root(tags.text("text", "Loading...") :: Nil)

  private def wrapDBView[R](view: ()=>R): R =
    eventSource.incrementalApplyAndView { () ⇒
      val startTime = System.currentTimeMillis
      val res = view()
      val endTime = System.currentTimeMillis
      currentVDom.until(endTime+(endTime-startTime)*10)
      res
    }

  private def listView(pf: String) = wrapDBView{ ()=>
    ???
  }
/*
  private def editView(pf: String) = wrapDBView{ ()=>
    ???
  }
*/
  def handlers = CoHandler(ViewPath(""))(emptyView) ::
    CoHandler(ViewPath("/list"))(listView) ::
    //CoHandler(ViewPath("/edit"))(listView) ::
    Nil
}
