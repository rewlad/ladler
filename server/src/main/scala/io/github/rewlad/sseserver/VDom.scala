package io.github.rewlad.sseserver

trait ToJson {
  def appendJson(builder: JsonBuilder): Unit
}
/*
trait Key extends ToJson {
  def jsonKey: String
  def appendJson(builder: JsonBuilder) = builder.append(jsonKey)
}*/
trait Value extends ToJson
trait Pair {
  def jsonKey: String
  def sameKey(other: Pair): Boolean
  def value: Value
  def withValue(value: Value): Pair
}
///

case class MapValue(value: List[Pair]) extends Value {
  def appendJson(builder: JsonBuilder) = {
    builder.startObject()
    value.foreach{ p =>
      builder.append(p.jsonKey)
      p.value.appendJson(builder)
    }
    builder.end()
  }
}
//object DoSetKey extends Key { def jsonKey = "$set" }
case class DoSetPair(value: Value) extends Pair {
  def jsonKey = "$set"
  def sameKey(other: Pair) = Never()
  def withValue(value: Value) = Never()
}
object Diff {
  private def value(l: List[Pair]) = l.head.value
  private def wasKeyDel(previous: List[Pair], current: List[Pair]): Boolean =
    if(previous.isEmpty) false else if(current.isEmpty) true
    else if(current.head.sameKey(previous.head)) wasKeyDel(previous.tail, current.tail)
    else wasKeyDel(previous, current.tail)

  private def diffList(previous: List[Pair], current: List[Pair]): List[Pair] = {
    if(current.isEmpty) return Nil
    if(previous.isEmpty || !current.head.sameKey(previous.head))
      return set(current) :: diffList(previous, current.tail)
    val tail = diffList(previous.tail, current.tail)
    value(previous) match {
      case p: MapValue => value(current) match {
        case n: MapValue =>
          val diff = apply(p,n)
          if(diff.isEmpty) tail else current.head.withValue(diff.get) :: tail
        case n => set(current) :: tail
      }
      case p if p == value(current) => tail
      case p => set(current) :: tail
    }
  }
  private def set(current: List[Pair]): Pair =
    current.head.withValue(set(value(current)))
  private def set(current: Value): MapValue = MapValue(DoSetPair(current)::Nil)
  def apply(previous: MapValue, current: MapValue): Option[Value] = {
    if(wasKeyDel(previous.value, current.value)) return Some(set(current))
    val diff = diffList(previous.value, current.value)
    if(diff.isEmpty) None else Some(MapValue(diff))
  }
}

///
case class ChildOrderPair(value: Value) extends Pair {
  def jsonKey = "chl"
  def sameKey(other: Pair) = other match {
    case v: ChildOrderPair => true
    case _ => false
  }
  def withValue(value: Value) = copy(value=value)
}
case class ChildOrderValue(value: List[Long]) extends Value {
  def appendJson(builder: JsonBuilder) = {
    if(value.size != value.distinct.size)
      throw new Exception(s"duplicate keys: $value")

    builder.startArray()
    value.foreach(k => builder.append(s":$k"))
    builder.end()
  }
}
object WithChildren {
  def apply[C](
    theElement: ElementValue,
    elements: List[Child[C]]
  ): MapValue = MapValue(
    TheElementPair(theElement) ::
      ChildOrderPair(ChildOrderValue(elements.map(_.key))) :: elements
  )
}
case class Child[C](key: Long, value: Value) extends Pair {
  def jsonKey = s":$key"
  def sameKey(other: Pair) = other match {
    case o: Child[_] => key == o.key
    case _ => false
  }
  def withValue(value: Value) = copy(value=value)
}
case class TheElementPair(value: Value) extends Pair {
  def jsonKey = "at"
  def sameKey(other: Pair) = other match {
    case v: TheElementPair => true
    case _ => false
  }
  def withValue(value: Value) = copy(value=value)
}
abstract class ElementValue extends Value {
  def elementType: String
  def appendJsonAttributes(builder: JsonBuilder): Unit
  def appendJson(builder: JsonBuilder) = {
    builder.startObject()
      .append("tp").append(elementType)
      .append("key").append("")
      appendJsonAttributes(builder)
    builder.end()
  }
  def handleMessage(message: ReceivedMessage): Unit
}
/*
object EmptyAttributesValue extends ElementValue {
  def appendJsonAttributes(builder: JsonBuilder) = ()
  def handleMessage(message: ReceivedMessage) = Never()
}*/