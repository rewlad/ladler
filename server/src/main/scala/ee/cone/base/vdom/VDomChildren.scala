package ee.cone.base.vdom

import ee.cone.base.util.Never
import ee.cone.base.vdom.Types.VDomKey

case class ChildOrderPair(value: VDomValue) extends VPair { //priv
  def jsonKey = "chl"
  def sameKey(other: VPair) = other match {
    case v: ChildOrderPair => true
    case _ => false
  }
  def withValue(value: VDomValue) = copy(value=value)
}
case class ChildOrderValue(value: List[VDomKey]) extends VDomValue { //priv
  def appendJson(builder: JsonBuilder) = {
    if(value.size != value.distinct.size)
      throw new Exception(s"duplicate keys: $value")

    builder.startArray()
    value.foreach(key => builder.append(LongJsonKey(key)))
    builder.end()
  }
}

class ChildPairFactoryImpl(createMapValue: List[VPair]=>MapVDomValue) extends ChildPairFactory {
  def apply[C](
    key: VDomKey,
    theElement: VDomValue,
    elements: List[ChildPair[_]]
  ): ChildPair[C] = ChildPairImpl[C](key, createMapValue(
    TheKeyPair :: TheElementPair(theElement) :: (
      if(elements.isEmpty) Nil
      else ChildOrderPair(ChildOrderValue(elements.map(_.key))) :: elements
    )
  ))
}

object LongJsonKey { def apply(key: VDomKey) = s":$key" }
case class ChildPairImpl[C](key: VDomKey, value: VDomValue) extends ChildPair[C] { //pub
  def jsonKey = LongJsonKey(key)
  def sameKey(other: VPair) = other match {
    case o: ChildPair[_] => key == o.key
    case _ => false
  }
  def withValue(value: VDomValue) = copy(value=value)
}

object EmptyStringValue extends VDomValue {
  def appendJson(builder: JsonBuilder) = builder.append("")
}

object TheKeyPair extends VPair {
  def jsonKey = "key"
  def sameKey(other: VPair) = this == other
  def value = EmptyStringValue
  def withValue(value: VDomValue) = Never()
}

case class TheElementPair(value: VDomValue) extends VPair { //priv
  def jsonKey = "at"
  def sameKey(other: VPair) = other match {
    case v: TheElementPair => true
    case _ => false
  }
  def withValue(value: VDomValue) = copy(value=value)
}


