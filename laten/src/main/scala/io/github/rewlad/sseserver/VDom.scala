package io.github.rewlad.sseserver

trait Key { def toStringKey: String }
sealed trait Value

case class MapValue(value: List[(Key,Value)]) extends Value
case class OrderValue(value: Seq[Key]) extends Value
case class StringValue(value: String) extends Value


object DoSetKey extends Key { def toStringKey = "$set" }
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

object AttributesKey extends Key { def toStringKey = "at" }
object ChildOrderKey extends Key { def toStringKey = "chl" }
abstract class ElementKey extends Key {
  def key: Int
  def elementType: String
  def toStringKey = s"$key:$elementType"
}
object Children {
  def apply(elements: List[(ElementKey,MapValue)]): List[(Key,Value)] = {
    val ordered = elements.map(_._1)
    /*if(ordered.size != ordered.distinct.size)
      throw new Exception(s"duplicate keys: $ordered")*/
    (ChildOrderKey -> OrderValue(ordered)) :: elements
  }
}

object ToJson {
  def apply(builder: JsonBuilder, value: Value): Unit = value match {
    case StringValue(v) => builder.append(v)
    case MapValue(v) =>
      builder.startObject()
      v.toArray.map{ case(k,vv) => (k.toStringKey, vv) }.sortBy(_._1).foreach{ case(k,vv) =>
        builder.append(k)
        apply(builder, vv)
      }
      builder.end()
    case OrderValue(v) =>
      builder.startArray()
      v.foreach(k => builder.append(k.toStringKey))
      builder.end()
  }
}
