package ee.cone.base.test_layout

import ee.cone.base.connection_api.{CoHandlerProvider, CoHandler}
import ee.cone.base.vdom.{Tags, ViewPath}

class TestComponent(
  tags: Tags,
  flexTags: FlexTags,
  materialTags: MaterialTags
) extends CoHandlerProvider {
  import tags._
  import flexTags._
  import materialTags._
  private def emptyView(pf: String) =
    List(text("text", "Loading..."))
  private def testView(pf: String) =
    List(
      paper("1",
        flexGrid("1",
          flexItem("2", 200, Some(300), paper("1", text("1", "content:ahaha1") :: Nil) :: Nil) ::
          flexItem("3", 900, Some(1000), paper("1", text("1", "content:ahaha2") :: Nil) :: Nil) ::
          Nil
        ) :: Nil
      )
    )


  def handlers =
    CoHandler(ViewPath(""))(emptyView) ::
    CoHandler(ViewPath("/test"))(testView) :: Nil
}
