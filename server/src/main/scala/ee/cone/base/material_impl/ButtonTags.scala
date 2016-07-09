package ee.cone.base.material_impl

import ee.cone.base.material.ButtonTags
import ee.cone.base.vdom.Types._
import ee.cone.base.vdom.{TagName, _}

case class IconButton(tooltip: String)(
  val onClick: Option[()⇒Unit]
) extends VDomValue with OnClickReceiver {
  def appendJson(builder: JsonBuilder) = {
    builder.startObject()
    builder.append("tp").append("IconButtonEx")
    if(tooltip.nonEmpty) builder.append("tooltip").append(tooltip) //todo: color str
    onClick.foreach(_⇒ builder.append("onClick").append("send"))
    builder.end()
  }
}

case class RaisedButton(label: String)(
  val onClick: Option[()⇒Unit]
) extends VDomValue with OnClickReceiver {
  def appendJson(builder: JsonBuilder) = {
    builder.startObject()
    builder.append("tp").append("RaisedButton")
    builder.append("primary").append(true)
    builder.append("label").append(label)
    onClick.foreach(_⇒ builder.append("onClick").append("send")) //try
    builder.end()
  }
}

case class CheckBox(checked:Boolean,label:String)(
  val onChange: Option[String=>Unit]
) extends VDomValue with OnChangeReceiver {
  def appendJson(builder: JsonBuilder)={
    builder.startObject()
    builder.append("tp").append("Checkbox")
    builder.append("labelPosition").append("left")
    builder.append("onCheck").append("send")
    builder.append("checked").append(checked)
    builder.append("label").append(label)
    builder.append("labelStyle").startObject()
    builder.append("fontSize").append("12px")
    builder.append("color").append("rgba(0,0,0,0.3)")
    builder.end()
    builder.end()
  }
}

class ButtonTagsImpl(
  child: ChildPairFactory, utils: TagJsonUtils, tags: Tags,
  style: TagStyles
) extends ButtonTags {
  import tags._
  def checkBox(key:VDomKey,label:String,checked:Boolean,check:Boolean=>Unit) = //inp,cust
    child[OfDiv](key,CheckBox(checked,label)(Some(v⇒check(v.nonEmpty))),Nil)
  def raisedButton(key: VDomKey, label: String)(action: ()=>Unit) = //inp,cust
    child[OfDiv](key, RaisedButton(label)(Some(action)), Nil)
  def iconButton(key: VDomKey, tooltip: String, picture: TagName)(action: ()=>Unit) =
    child[OfDiv](key, IconButton(tooltip)(Some(action)), List(tag("icon",picture)(Nil)))
}
