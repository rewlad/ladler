package ee.cone.base.test_loots

import ee.cone.base.connection_api.DictMessage
import ee.cone.base.vdom._
import ee.cone.base.vdom.Types._

case class Paper() extends VDomValue {
  def appendJson(builder: JsonBuilder) = {
    builder.startObject()
    builder.append("tp").append("Paper") //style?
    builder.end()
  }
}

case class Table() extends VDomValue {
  def appendJson(builder: JsonBuilder) = {
    builder.startObject()
    builder.append("tp").append("Table")
    builder.append("fixedHeader").append(false)
    builder.append("displayRowCheckbox").append(false) //?
    builder.end()
  }
}

case class TableHeader() extends VDomValue {
  def appendJson(builder: JsonBuilder) = {
    builder.startObject()
    builder.append("tp").append("TableHeader")
    builder.append("adjustForCheckbox").append(true)
    builder.append("displaySelectAll").append(false)
    builder.end()
  }
}

case class TableBody() extends VDomValue {
  def appendJson(builder: JsonBuilder) = {
    builder.startObject()
    builder.append("tp").append("TableBody")
    builder.append("displayRowCheckbox").append(true) //?
    builder.end()
  }
}

case class TableRow() extends VDomValue {
  def appendJson(builder: JsonBuilder) = {
    builder.startObject()
    builder.append("tp").append("TableRow") //selectable?
    builder.end()
  }
}

case class TableColumn(isHead: Boolean, isRight: Boolean, colSpan: Int, content: String) extends VDomValue {
  def appendJson(builder: JsonBuilder) = {
    builder.startObject()
    builder.append("tp").append(if(isHead) "TableHeaderColumn" else "TableRowColumn")
    if(colSpan != 1) builder.append("colSpan").append(colSpan.toString) //may be append int or transform
    builder.append("style").startObject()
    builder.append("textAlign").append(if(isRight) "right" else "left")
    builder.end()
    if(content.nonEmpty) builder.append("content").append(content)
    builder.end()
  }
}

case class IconButton(tooltip: String)(
  val receive: PartialFunction[DictMessage,Unit]
) extends VDomValue {
  def appendJson(builder: JsonBuilder) = {
    builder.startObject()
    builder.append("tp").append("IconButton")
    if(tooltip.nonEmpty) builder.append("tooltip").append(tooltip)
    builder.append("onClick").append("send") //? will it work
    builder.end()
  }
}

case class SVGIcon(tp: String) extends VDomValue {
  def appendJson(builder: JsonBuilder) = {
    builder.startObject()
    builder.append("tp").append(tp) //?color
    builder.end()
  }
}

trait OfTable
trait OfTableRow

class MaterialTags(
  child: ChildPairFactory
) {
  def paper(key: VDomKey, children: List[ChildPair[OfDiv]]) =
    child[OfDiv](key, Paper(), children)

  def table(key: VDomKey, head: List[ChildPair[OfTable]], body: List[ChildPair[OfTable]]) =
    child[OfDiv](key, Table(),
      child("head",TableHeader(), head) :: child("body",TableBody(), body) :: Nil
    )
  def row(key: VDomKey, children: List[ChildPair[OfTableRow]]) =
    child[OfTable](key, TableRow(), children)
  def th(key: VDomKey, isRight: Boolean, colSpan: Int, children: List[ChildPair[OfDiv]]) =
    child[OfTableRow](key, TableColumn(isHead = true, isRight, colSpan, ""), children)
  def th(key: VDomKey, isRight: Boolean, children: List[ChildPair[OfDiv]]) =
    child[OfTableRow](key, TableColumn(isHead = true, isRight, 1, ""), children)
  def th(key: VDomKey, isRight: Boolean, content: String) =
    child[OfTableRow](key, TableColumn(isHead = true, isRight, 1, content), Nil)
  def td(key: VDomKey, isRight: Boolean, children: List[ChildPair[OfDiv]]) =
    child[OfTableRow](key, TableColumn(isHead = false, isRight, 1, ""), children)
  def td(key: VDomKey, isRight: Boolean, content: String) =
    child[OfTableRow](key, TableColumn(isHead = false, isRight, 1, content), Nil)

  //def iconFilterList()
  def iconButton(key: VDomKey, tooltip: String, picture: String, action: ()=>Unit) =
    child[OfDiv](key, IconButton(tooltip)(???),
      child("icon", SVGIcon(picture), Nil) :: Nil
    )


}
