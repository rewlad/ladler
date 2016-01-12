package io.github.rewlad.sseserver

trait ToJson {
  def appendJson(builder: JsonBuilder): Unit
}
trait Key extends ToJson
trait Value extends ToJson

///

case class MapValue(value: List[(Key,Value)]) extends Value {
  def appendJson(builder: JsonBuilder) = {
    builder.startObject()
    value.foreach{ case(k,v) =>
      k.appendJson(builder)
      v.appendJson(builder)
    }
    builder.end()
  }
}
object DoSetKey extends Key {
  def appendJson(builder: JsonBuilder) = builder.append("$set")
}
object Diff {
  private def key(l: List[(Key,Value)]) = l.head._1
  private def value(l: List[(Key,Value)]) = l.head._2
  private def wasKeyDel(previous: List[(Key,Value)], current: List[(Key,Value)]): Boolean =
    if(previous.isEmpty) false else if(current.isEmpty) true
    else if(key(previous) == key(current)) wasKeyDel(previous.tail, current.tail)
    else wasKeyDel(previous, current.tail)

  private def diffList(previous: List[(Key,Value)], current: List[(Key,Value)]): List[(Key,Value)] = {
    if(current.isEmpty) return Nil
    if(previous.isEmpty || key(previous) != key(current))
      return set(current) :: diffList(previous, current.tail)
    val tail = diffList(previous.tail, current.tail)
    value(previous) match {
      case p: MapValue => value(current) match {
        case n: MapValue =>
          val diff = apply(p,n)
          if(diff.isEmpty) tail else (key(current) -> diff.get) :: tail
        case n => set(current) :: tail
      }
      case p if p == value(current) => tail
      case p => set(current) :: tail
    }
  }
  private def set(current: List[(Key,Value)]): (Key,Value) =
    key(current) -> set(value(current))
  private def set(current: Value): MapValue = MapValue((DoSetKey->current)::Nil)
  def apply(previous: MapValue, current: MapValue): Option[Value] = {
    if(wasKeyDel(previous.value, current.value)) return Some(set(current))
    val diff = diffList(previous.value, current.value)
    if(diff.isEmpty) None else Some(MapValue(diff))
  }
}

///
abstract class ElementKey extends Key {
  def key: Int
  def elementType: String
  def appendJson(builder: JsonBuilder) = builder.append(s"$key:$elementType")
}
object ChildOrderKey extends Key {
  def appendJson(builder: JsonBuilder) = builder.append("chl")
}
case class ChildOrderValue(value: List[ElementKey]) extends Value {
  def appendJson(builder: JsonBuilder) = {
    if(value.size != value.distinct.size)
      throw new Exception(s"duplicate keys: $value")

    builder.startArray()
    value.foreach(k => k.appendJson(builder))
    builder.end()
  }
}
object Children {
  def apply(elements: List[(ElementKey,Value)]): List[(Key,Value)] =
    (ChildOrderKey -> ChildOrderValue(elements.map(_._1))) :: elements
}

///

object AttributesKey extends Key {
  def appendJson(builder: JsonBuilder) = builder.append("at")
}