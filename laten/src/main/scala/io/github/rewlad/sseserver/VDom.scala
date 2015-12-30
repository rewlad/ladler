package io.github.rewlad.sseserver

import java.util

trait Element {
  def key: Int
  def elementType: String
  def childElements: Array[Element]
  def appendToJson(builder: JsonBuilder): Unit
}

object Diff {
  def sameKeys(previous: Array[Element], current: Array[Element]): Boolean = {
    if(previous.length != current.length) return false
    var j = current.length - 1
    while(j >= 0){
      if(previous(j).key != current(j).key) return false
      if(previous(j).elementType != current(j).elementType) return false
      j -= 1
    }
    true
  }
  def apply(previous: Array[Element], current: Array[Element]) = {
    if(sameKeys(previous,current)){}

    val previousPairs = previous.map(e=>(e.elementType,e.key)->e)
    val previousMap = previousPairs.toMap
    val currentPairs = current.map(e=>(e.elementType,e.key)->e)
    val currentMap = currentPairs.toMap
    val del = previousPairs.filter{ case(k,_) => !currentMap.contains(k) }
    val upd = currentPairs.flatMap{ case(k,e) =>
      Diff(previousMap.get(k), e).map(k->_)
    }

  }
  def apply(previous: Option[Element], current: Element): Option[Element] =
    if(previous.isEmpty) set(current) else {








    }

}





case class MapValue(value: Map[Key,Value]) extends Value
case class OrderValue(value: Seq[Key]) extends Value
case class StringValue(value: String) extends Value

object DoDeleteKey extends Key { def toStringKey = "$delete" }
object DoSetKey extends Key { def toStringKey = "$set" }
object Diff {
  private def map(previous: MapValue, current: MapValue): Option[Value] = {
    //val del = previous.value.keySet -- current.value.keySet
    //val del = previous.value.keysIterator.filter(k => !current.value.contains(k)).toSeq
    val del = previous.value.keysIterator.filterNot(current.value.contains).toSeq
    val upd = current.value.flatMap{ case(k,v) =>
      Diff(previous.value.get(k), v).map(k->_)
    }
    val changes = if(del.nonEmpty)
      upd + (DoDeleteKey->OrderValue(del.sortBy(_.toStringKey))) else upd
    if(changes.nonEmpty) Some(MapValue(changes)) else None
  }
  private def set(current: Value) = Some(MapValue(Map(DoSetKey->current)))
  def apply(previous: Option[Value], current: Value): Option[Value] =
    if(previous.isEmpty) set(current) else previous.get match {
      case p: MapValue => current match {
        case n: MapValue => map(p, n)
        case n => set(current)
      }
      case p => if(p == current) None else set(current)
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
