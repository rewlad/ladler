package ee.cone.base.vdom

import ee.cone.base.util.{Setup, Never}

case class DoSetPair(value: Value) extends VPair {
  def jsonKey = "$set"
  def sameKey(other: VPair) = Never()
  def withValue(value: Value) = Never()
}
object WasNoValue extends Value {
  override def appendJson(builder: JsonBuilder): Unit = Never()
}

class DiffImpl(createMapValue: List[VPair]=>MapValue) extends Diff {
  var prevVDom: Value = WasNoValue
  def diff(vDom: Value): Option[MapValue] = if(prevVDom eq vDom) None
    else Setup(diff(prevVDom, vDom)){ _ => prevVDom = vDom }
  private def set(value: Value) = Some(createMapValue(DoSetPair(value)::Nil))
  def diff(prevValue: Value, currValue: Value): Option[MapValue] = prevValue match {
    case p: MapValue => currValue match {
      case n: MapValue =>
        var previous = p.pairs
        var current  = n.pairs
        var res: List[VPair] = Nil
        while(current.nonEmpty){
          if(previous.isEmpty || !current.head.sameKey(previous.head))
            previous = current.head.withValue(WasNoValue) :: previous
          val d = diff(previous.head.value, current.head.value)
          if (d.nonEmpty) res = current.head.withValue(d.get) :: res
          previous = previous.tail
          current = current.tail
        }
        if(previous.nonEmpty) set(n)
        else if(res.nonEmpty) Some(createMapValue(res))
        else None
      case n => set(currValue)
    }
    case p if p == currValue => None
    case p => set(currValue)
  }
}
