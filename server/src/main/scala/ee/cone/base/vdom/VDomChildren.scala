package ee.cone.base.vdom

import ee.cone.base.util.Never
import ee.cone.base.vdom.Types.VDomKey

case class ChildOrderPair(value: Value) extends VPair { //priv
  def jsonKey = "chl"
  def sameKey(other: VPair) = other match {
    case v: ChildOrderPair => true
    case _ => false
  }
  def withValue(value: Value) = copy(value=value)
}
case class ChildOrderValue(value: List[VDomKey]) extends Value { //priv
  def appendJson(builder: JsonBuilder) = {
    if(value.size != value.distinct.size)
      throw new Exception(s"duplicate keys: $value")

    builder.startArray()
    value.foreach(key => builder.append(LongJsonKey(key)))
    builder.end()
  }
}

class ChildPairFactoryImpl(createMapValue: List[VPair]=>MapValue) extends ChildPairFactory {
  def apply[C](
    key: VDomKey,
    theElement: Value,
    elements: List[ChildPair[_]]
  ): ChildPair[C] = ChildPairImpl[C](key, createMapValue(
    TheKeyPair :: TheElementPair(theElement) :: (
      if(elements.isEmpty) Nil
      else ChildOrderPair(ChildOrderValue(elements.map(_.key))) :: elements
    )
  ))
}

object LongJsonKey { def apply(key: VDomKey) = s":$key" }
case class ChildPairImpl[C](key: VDomKey, value: Value) extends ChildPair[C] { //pub
  def jsonKey = LongJsonKey(key)
  def sameKey(other: VPair) = other match {
    case o: ChildPair[_] => key == o.key
    case _ => false
  }
  def withValue(value: Value) = copy(value=value)
}

object EmptyStringValue extends Value {
  def appendJson(builder: JsonBuilder) = builder.append("")
}

object TheKeyPair extends VPair {
  def jsonKey = "key"
  def sameKey(other: VPair) = this == other
  def value = EmptyStringValue
  def withValue(value: Value) = Never()
}

case class TheElementPair(value: Value) extends VPair { //priv
  def jsonKey = "at"
  def sameKey(other: VPair) = other match {
    case v: TheElementPair => true
    case _ => false
  }
  def withValue(value: Value) = copy(value=value)
}


