package io.github.rewlad.sseserver

case class ChildOrderPair(value: Value) extends Pair { //priv
  def jsonKey = "chl"
  def sameKey(other: Pair) = other match {
    case v: ChildOrderPair => true
    case _ => false
  }
  def withValue(value: Value) = copy(value=value)
}
case class ChildOrderValue(value: List[Long]) extends Value { //priv
  def appendJson(builder: JsonBuilder) = {
    if(value.size != value.distinct.size)
      throw new Exception(s"duplicate keys: $value")

    builder.startArray()
    value.foreach(key => builder.append(LongJsonKey(key)))
    builder.end()
  }
}

object Child { //pub
  def apply[C](key: Long, theElement: ElementValue) = ChildPair[C](key, theElement)
  def apply[C](
    key: Long,
    theElement: ElementValue,
    elements: List[ChildPair[_]]
  ): ChildPair[C] = ChildPair[C](key, MapValue(
    TheElementPair(theElement) ::
      ChildOrderPair(ChildOrderValue(elements.map(_.key))) :: elements
  ))
}
object LongJsonKey { def apply(key: Long) = s":$key" }
case class ChildPair[C](key: Long, value: Value) extends Pair { //pub
  def jsonKey = LongJsonKey(key)
  def sameKey(other: Pair) = other match {
    case o: ChildPair[_] => key == o.key
    case _ => false
  }
  def withValue(value: Value) = copy(value=value)
}

case class TheElementPair(value: Value) extends Pair { //priv
  def jsonKey = "at"
  def sameKey(other: Pair) = other match {
    case v: TheElementPair => true
    case _ => false
  }
  def withValue(value: Value) = copy(value=value)
}

abstract class ElementValue extends Value { //pub
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
