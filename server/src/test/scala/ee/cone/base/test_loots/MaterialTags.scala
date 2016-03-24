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
    builder.append("tp").append("Table")
    //builder.append("fixedHeader").append(false)
    builder.append("displayRowCheckbox").append(false) //try
    builder.end()
  }
}

case class TableHeader() extends VDomValue {
  def appendJson(builder: JsonBuilder) = {
    builder.startObject()
    builder.append("tp").append("TableHeader")
    builder.append("adjustForCheckbox").append(true) //for indent
    builder.append("displaySelectAll").append(false)
    builder.end()
  }
}

case class TableBody() extends VDomValue {
  def appendJson(builder: JsonBuilder) = {
    builder.startObject()
    builder.append("tp").append("TableBody")
    builder.append("displayRowCheckbox").append(true) //try
    builder.end()
  }
}

case class TableRow() extends VDomValue {
  def appendJson(builder: JsonBuilder) = {
    builder.startObject()
    builder.append("tp").append("TableRow") // selectable -- checkbox enabled
    builder.end()
  }
}

case class TableColumn(isHead: Boolean, isRight: Boolean, colSpan: Int) extends VDomValue {
  def appendJson(builder: JsonBuilder) = {
    builder.startObject()
    builder.append("tp").append(if(isHead) "TableHeaderColumn" else "TableRowColumn")
    if(colSpan != 1) builder.append("colSpan").append(colSpan.toString) //may be append int or transform
    builder.append("style"); {
      builder.startObject()
      builder.append("textAlign").append(if(isRight) "right" else "left")
      builder.end()
    }
    builder.end()
  }
}

case class IconButton(tooltip: String)(
  val receive: PartialFunction[DictMessage,Unit]
) extends VDomValue {
  def appendJson(builder: JsonBuilder) = {
    builder.startObject()
    builder.append("tp").append("IconButton")
    if(tooltip.nonEmpty) builder.append("tooltip").append(tooltip) //todo: color str
    builder.append("onClick").append("send") //? will it work
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
  input: InputAttributes, convertToString: Value⇒String
)(
  val receive: PartialFunction[DictMessage,Unit]
) extends VDomValue {
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
  val receive: PartialFunction[DictMessage,Unit]
) extends VDomValue {
  def appendJson(builder: JsonBuilder) = {
    builder.startObject()
    builder.append("tp").append("RaisedButton")
    builder.append("secondary").append(true)
    builder.append("onClick").append("send") //? will it work
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
  child: ChildPairFactory, inputAttributes: InputAttributes,
  onClick: OnClick, onChange: OnChange
) {
  def paper(key: VDomKey, children: ChildPair[OfDiv]*) =
    child[OfDiv](key, Paper(), children.toList)

  def table(key: VDomKey, head: List[ChildPair[OfTable]], body: List[ChildPair[OfTable]]) =
    child[OfDiv](key, Table(),
      child("head",TableHeader(), head) :: child("body",TableBody(), body) :: Nil
    )
  def row(key: VDomKey, children: ChildPair[OfTableRow]*) =
    child[OfTable](key, TableRow(), children.toList)
  def cell(key: VDomKey, isHead: Boolean=false, isRight: Boolean=false, colSpan: Int=1)(children: List[ChildPair[OfDiv]]=Nil) =
    child[OfTableRow](key, TableColumn(isHead, isRight, colSpan), children)

  private def iconButton(key: VDomKey, tooltip: String, picture: String, action: ()=>Unit) =
    child[OfDiv](key,
      IconButton(tooltip){ case `onClick`() => action() },
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
    child[OfDiv](key, RaisedButton(label){ case `onClick`() => action() }, Nil)

  def textInput(label: String, value: String, change: String⇒Unit) =
    InputField("TextField", label, value, deferSend = true)(inputAttributes, identity){
      case `onChange`(newValue) ⇒ change(newValue)
    }

  private def instantToString(value: Instant): String =
    value.getEpochSecond.toString
  private def stringToInstant(value: String): Instant =
    new Instant(java.lang.Long.valueOf(value),0)
  def dateInput(label: String, value: Instant, change: Instant⇒Unit) =
    InputField("DateInput", label, value, deferSend = false)(inputAttributes, instantToString){
      case `onChange`(newValue) ⇒ change(stringToInstant(newValue))
    }


}