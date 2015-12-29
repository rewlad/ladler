package io.github.rewlad.sseserver

trait Key { def toStringKey: String }
sealed trait Value

case class MapValue(value: Map[Key,Value]) extends Value
case class OrderValue(value: Seq[Key]) extends Value
case class StringValue(value: String) extends Value

object DoDeleteKey extends Key { def toStringKey = "$delete" }
object DoSetKey extends Key { def toStringKey = "$set" }
object Diff {
  private object NoValue extends Value
  private def default(previous: Value, current: Value): Seq[Value] =
    if(previous == current) Nil else MapValue(Map(DoSetKey->current)) :: Nil
  private def map(previous: MapValue, current: MapValue): Seq[Value] = {
    val del = previous.value.keys.filterNot(current.value.contains)
    val upd = current.value.flatMap{ case(k,v) =>
      Diff(previous.value.getOrElse(k,NoValue), v).map(k->_)
    }
    val changes = if(del.nonEmpty) upd + (DoDeleteKey->OrderValue(del.toSeq)) else upd
    if(changes.nonEmpty) MapValue(changes) :: Nil else Nil
  }
  def apply(previous: Value, current: Value): Seq[Value] = previous match {
    case p: MapValue => current match {
      case n: MapValue => map(p, n)
      case n => default(p, n)
    }
    case p => default(p, current)
  }
}

object AttributesKey extends Key { def toStringKey = "at" }
object ChildOrderKey extends Key { def toStringKey = "chl" }
abstract class ElementKey extends Key {
  def key: Int
  def elementType: String
  def toStringKey = s"$key:$elementType"
}
object Children {
  def apply(attributes: Map[Key,Value], elements: Seq[(ElementKey,MapValue)]): MapValue = {
    if(elements.isEmpty) return MapValue(attributes)
    val ordered = ChildOrderKey -> OrderValue(elements.map(_._1))
    val elementMap = elements.toMap[Key,Value]
    if(elementMap.size != elements.size)
      throw new Exception(s"duplicate keys: $ordered")
    if(attributes.isEmpty) return MapValue(elementMap + ordered)
    MapValue(elementMap + ordered + (AttributesKey -> MapValue(attributes)))
  }
}

case class SimpleElementKey(elementType: String, key: Int) extends ElementKey
object Tag {
  def apply(tagName: String, key: Int, attributes: Map[Key,Value], elements: Seq[(ElementKey,MapValue)]): (ElementKey,MapValue) =
    SimpleElementKey(tagName,key) -> Children(attributes,elements)
}

object ToJson {
  def apply(value: Value): String = {
    val sb = new StringBuilder
    apply(sb, value)
    sb.toString
  }
  def apply(sb: StringBuilder, value: Value): Unit = value match {
    case MapValue(v) =>
    case OrderValue(v) => v.foreach(k => apply(sb, k.toStringKey))
    case StringValue(v) => apply(sb, v)
  }


}
class JsonBuilder(sb: StringBuilder = new StringBuilder, state: StringBuilder = new StringBuilder) {
  private def mayBeDelimiter(): Unit = sb.charAt(sb.length - 1) match {
    case '[' | '{' | ':' => ()
    case _ => sb.append(',')
  }
  private def start(c: Char): Unit = {
    mayBeDelimiter()
    sb.append(c)
    state.append(c)
  }

  def startObject(): Unit = start('{')
  def startArray(): Unit = start('[')

  def end(): Unit = state.charAt(state.length - 1) match {
    sb.append()
    state.setLength(state.length - 1)

  }

  def append(value: String): Unit = {
    mayBeDelimiter()
    sb.append('"')
    var j = 0
    while(j < value.length){
      val c = value(j)
      if(c == '\\' || c == '"' ||  c < '\u0020')
        sb.append(if(c < '\u0010')"""\u000""" else """\u00""").append(Integer.toHexString(c))
      else
        sb.append(c)
      j += 1
    }
    sb.append('"')
  }

}



/*
object Diff {
  private def sameChildKeys(previous: OrderedChildren, current: OrderedChildren): Boolean = {
    if(previous.content.size != current.content.size) return false
    var j = previous.content.size - 1
    while(j >= 0) if()

      true
  }

  def apply(previous: OrderedChildren, current: OrderedChildren) = {

  }
    if(previous.active == current.active) {

    } else {

    }
}
*/
/*
object MakeImmChildren {
  def apply(c: Seq[(ChildKey,ImmChildren)]) = {
    val deep = c.filterNot(_==NoChildren).toMap
    val active = c.map(_._1)
    ImmChildren(active, deep)
  }
}
*/
/*
object NoChildren extends ImmChildren(Nil,Map())
case class ImmChildren(active: Seq[ChildKey], deep: Map[ChildKey,ImmChildren])

*/




/*
  */
/*{
//  def createChildren(): RxChildren
//  def transforming: Boolean
//  def detach(c: RxChildren): Unit
//  def toSeq:Seq[this.type] = Seq(this)
}*/
//if(c.size != deep.size) throw new Exception(s"duplicate keys: $active")





