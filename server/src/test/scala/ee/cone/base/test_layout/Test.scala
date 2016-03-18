package ee.cone.base.test_layout

import ee.cone.base.connection_api.CoHandler
import ee.cone.base.vdom.ViewPath

trait MapProp {
  def get(key:String): Any
  def set(key:String, value: Any): Unit
  def getVer():Int
}

class Test(
  tags: Tags
) {
  import tags._
  private def emptyView(pf: String) =
    tags.root(tags.text("text", "Loading...") :: Nil)
  private def testView(pf: String) = {
    root(
      div(//position: relative
        1, "", "ref:observer()",
        paper(
          flexGrid( //no childOfGrid => use grid item only
            "1", "ref:grid,paper:1// div to child:Paper",
            flexGridShItem(
              2,
              "ref:shchildofgrid//to flexGridShItem,flexBasis:200px,maxWidth:300px"

            ) ::
              flexGridShItem(
                3,
                "ref:shchildofgrid,flexBasis:900px//+Width,maxWidth:1000px"
              ) :: Nil
          ) ::

            flexGridItem(
              3,
              "paper:1,ref:childofgrid",
              div(1, "", "content:ahaha2", Nil) :: Nil
            ) ::
            flexGridItem(
              2,
              "paper:0,ref:childofgrid",
              div(1, "", "content:ahaha2", Nil) :: Nil
            ) :: Nil
        ) :: Nil
      )
    )
  }

  def handlers =
    CoHandler(ViewPath(""))(emptyView) ::
    CoHandler(ViewPath("/test"))(testView) :: Nil
}
