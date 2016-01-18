package io.github.rewlad.sseserver.test_react

class BigView(models: List[Model]) extends View {
  lazy val modelVersion = s"${System.currentTimeMillis / 100}"
  def generateDom = {
    import Tag._
    val size = 100
    root(
      table(0, (1 to size).map(trIndex =>
        tr(trIndex, (1 to size).map(tdIndex =>
          td(tdIndex,
            button(0,
              if(trIndex==25 && tdIndex==25) modelVersion
              else s"$trIndex/$tdIndex"
            ) :: Nil
          )
        ).toList)
      ).toList) :: Nil
    )
  }
}
