package io.github.rewlad.sseserver

case class DoSetPair(value: Value) extends Pair {
  def jsonKey = "$set"
  def sameKey(other: Pair) = Never()
  def withValue(value: Value) = Never()
}
object WasNoValue extends Value {
  override def appendJson(builder: JsonBuilder): Unit = Never()
}
object Diff {
  private def set(value: Value) = Some(MapValue(DoSetPair(value)::Nil))
  def apply(prevValue: Value, currValue: Value): Option[MapValue] = prevValue match {
    case p: MapValue => currValue match {
      case n: MapValue =>
        var previous = p.value
        var current  = n.value
        var res: List[Pair] = Nil
        while(current.nonEmpty){
          if(previous.isEmpty || !current.head.sameKey(previous.head))
            previous = current.head.withValue(WasNoValue) :: previous
          val d = apply(previous.head.value, current.head.value)
          if (d.nonEmpty) res = current.head.withValue(d.get) :: res
          previous = previous.tail
          current = current.tail
        }
        if(previous.nonEmpty) set(n)
        else if(res.nonEmpty) Some(MapValue(res))
        else None
      case n => set(currValue)
    }
    case p if p == currValue => None
    case p => set(currValue)
  }
}
