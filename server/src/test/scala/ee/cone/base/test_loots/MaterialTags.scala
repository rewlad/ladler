package ee.cone.base.test_loots

import java.time.Instant

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
    builder.append("tp").append("table")
    builder.append("style"); {
      builder.startObject()
      builder.append("borderCollapse").append("collapse")
      builder.end()
    }
    /*
    builder.append("tp").append("Table")
    //builder.append("fixedHeader").append(false)
    builder.append("displayRowCheckbox").append(false) //try
    */
    builder.end()
  }
}

case class TableHeader() extends VDomValue {
  def appendJson(builder: JsonBuilder) = {
    builder.startObject()
    builder.append("tp").append("thead")
    /*
    builder.append("tp").append("TableHeader")
    builder.append("adjustForCheckbox").append(true) //for indent
    builder.append("displaySelectAll").append(false)
    */
    builder.end()
  }
}

case class TableBody() extends VDomValue {
  def appendJson(builder: JsonBuilder) = {
    builder.startObject()
    builder.append("tp").append("tbody")
    /*
    builder.append("tp").append("TableBody")
    builder.append("displayRowCheckbox").append(true) //try
    */
    builder.end()
  }
}

case class TableRow() extends VDomValue {
  def appendJson(builder: JsonBuilder) = {
    builder.startObject()
    //builder.append("tp").append("TableRow") // selectable -- checkbox enabled
    builder.append("tp").append("tr")
    builder.end()
  }
}

case class TableColumn(isHead: Boolean, isRight: Boolean, colSpan: Int)(
    val onClick: Option[()⇒Unit]
) extends VDomValue with OnClickReceiver {
  def appendJson(builder: JsonBuilder) = {
    builder.startObject()
    //builder.append("tp").append(if(isHead) "TableHeaderColumn" else "TableRowColumn")
    builder.append("tp").append(if(isHead) "th" else "td")
    onClick.foreach(_⇒ builder.append("onClick").append("send"))
    if(colSpan != 1) builder.append("colSpan").append(colSpan.toString) //may be append int or transform
    builder.append("style"); {
      builder.startObject()
      builder.append("textAlign").append(if(isRight) "right" else "left")
      if(!isHead) builder.append("border-top").append("1px solid silver")
      builder.end()
    }
    builder.end()
  }
}

case class IconButton(tooltip: String)(
    val onClick: Option[()⇒Unit]
) extends VDomValue with OnClickReceiver {
  def appendJson(builder: JsonBuilder) = {
    builder.startObject()
    builder.append("tp").append("IconButton")
    if(tooltip.nonEmpty) builder.append("tooltip").append(tooltip) //todo: color str
    onClick.foreach(_⇒ builder.append("onClick").append("send"))
    builder.end()
  }
}

case class SVGIcon(tp: String) extends VDomValue {
  def appendJson(builder: JsonBuilder) = {
    builder.startObject()
    builder.append("tp").append(tp) //color str
    builder.end()
  }
}

case class MarginWrapper(value: Int) extends VDomValue {
  def appendJson(builder: JsonBuilder) = {
    builder.startObject()
    builder.append("tp").append("span")
    builder.append("style"); {
      builder.startObject()
      builder.append("margin").append(s"${value}px")
      builder.end()
    }
    builder.end()
  }
}

case class InputField[Value](tp: String, label: String, value: Value, deferSend: Boolean)(
  input: InputAttributes, convertToString: Value⇒String, val onChange: Option[String⇒Unit]
) extends VDomValue with OnChangeReceiver {
  def appendJson(builder: JsonBuilder) = {
    builder.startObject()
    builder.append("tp").append(tp)
    builder.append("floatingLabelText").append(label)
    input.appendJson(builder, convertToString(value), deferSend)
    builder.append("style"); {
      builder.startObject()
      builder.append("width").append("100%")
      builder.end()
    }
    builder.end()
  }
}

case class RaisedButton(label: String)(
    val onClick: Option[()⇒Unit]
) extends VDomValue with OnClickReceiver {
  def appendJson(builder: JsonBuilder) = {
    builder.startObject()
    builder.append("tp").append("RaisedButton")
    builder.append("secondary").append(true)
    onClick.foreach(_⇒ builder.append("onClick").append("send")) //try
    builder.end()
  }
}

  /*
//todo: DateField, SelectField
//todo: Helmet, tap-event, StickyToolbars
//todo: port: import MaterialChip      from './material-chip'

*/

trait OfTable
trait OfTableRow

class MaterialTags(
  child: ChildPairFactory, inputAttributes: InputAttributes
) {
  def paper(key: VDomKey, children: ChildPair[OfDiv]*) =
    child[OfDiv](key, Paper(), children.toList)

  def table(key: VDomKey, head: List[ChildPair[OfTable]], body: List[ChildPair[OfTable]]) =
    child[OfDiv](key, Table(),
      child("head",TableHeader(), head) ::
      child("body",TableBody(), body) :: Nil
    )
  def row(key: VDomKey, children: ChildPair[OfTableRow]*) =
    child[OfTable](key, TableRow(), children.toList)
  def cell(key: VDomKey, isHead: Boolean=false, isRight: Boolean=false, colSpan: Int=1)(children: List[ChildPair[OfDiv]]=Nil, action: Option[()⇒Unit]=None) =
    child[OfTableRow](key, TableColumn(isHead, isRight, colSpan)(action), children)

  private def iconButton(key: VDomKey, tooltip: String, picture: String, action: ()=>Unit) =
    child[OfDiv](key,
      IconButton(tooltip)(Some(action)),
      child("icon", SVGIcon(picture), Nil) :: Nil
    )
  def btnFilterList(key: VDomKey, action: ()=>Unit) =
    iconButton(key,"filters","IconContentFilterList",action)
  def btnClear(key: VDomKey, action: ()=>Unit) =
    iconButton(key,"clear sorting","IconContentClear",action)
  def btnAdd(key: VDomKey, action: ()=>Unit) =
    iconButton(key,"","IconContentAdd",action)
  def btnRemove(key: VDomKey, action: ()=>Unit) =
    iconButton(key,"","IconContentRemove",action)

  def withMargin(key: VDomKey, value: Int, theChild: ChildPair[OfDiv]) =
    child[OfDiv](key, MarginWrapper(value), theChild :: Nil)

  def btnRaised(key: VDomKey, label: String)(action: ()=>Unit) =
    child[OfDiv](key, RaisedButton(label)(Some(action)), Nil)

  def textInput(key: VDomKey, label: String, value: String, change: String⇒Unit) =
    child[OfDiv](key, InputField("TextField", label, value, deferSend = true)(inputAttributes, identity, Some(newValue ⇒ change(newValue))), Nil)

  private def instantToString(value: Option[Instant]): String =
    value.map(_.getEpochSecond.toString).getOrElse("")
  private def stringToInstant(value: String): Option[Instant] =
    if(value.nonEmpty) Some(Instant.ofEpochSecond(java.lang.Long.valueOf(value),0)) else None
  def dateInput(key: VDomKey, label: String, value: Option[Instant], change: Option[Instant]⇒Unit) =
    child[OfDiv](key, InputField("DateInput", label, value, deferSend = false)(inputAttributes, instantToString, Some(newValue ⇒ change(stringToInstant(newValue)))), Nil)


}