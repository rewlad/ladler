package io.github.rewlad.sseserver

case class DoSetPair(value: Value) extends Pair {
  def jsonKey = "$set"
  def sameKey(other: Pair) = Never()
  def withValue(value: Value) = Never()
}
object Diff {
  private def wasKeyDel(previous: List[Pair], current: List[Pair]): Boolean =
    if(previous.isEmpty) false else if(current.isEmpty) true
    else if(current.head.sameKey(previous.head)) wasKeyDel(previous.tail, current.tail)
    else wasKeyDel(previous, current.tail)
  private def set(currPair: Pair): Pair =
    currPair.withValue(MapValue(DoSetPair(currPair.value)::Nil))
  private def set(currValue: Value): Option[MapValue] =
    Some(MapValue(DoSetPair(currValue)::Nil))
  private def diffList(previous: List[Pair], current: List[Pair]): List[Pair] = {
    if(current.isEmpty) return Nil
    if(previous.isEmpty || !current.head.sameKey(previous.head))
      return set(current.head) :: diffList(previous, current.tail)
    val tail = diffList(previous.tail, current.tail)
    diff(previous.head, current.head).map(_::tail).getOrElse(tail)
  }
  private def diff(prevValue: Value, currValue: Value): Option[MapValue] = prevValue match {
    case p: MapValue => currValue match {
      case n: MapValue if wasKeyDel(p.value, n.value) => set(currValue)
      case n: MapValue =>
        val diff = diffList(p.value, n.value)
        if(diff.isEmpty) None else Some(MapValue(diff))
      case n => set(currValue)
    }
    case p if p == currValue => None
    case p => set(currValue)
  }

  def diff(prevPair: Pair, currPair: Pair): Option[Pair] =
    diff(prevPair.value, currPair.value).map(currPair.withValue)

  def apply(prevPairOpt: Option[Pair], currPair: Pair)(thenDo: MapValue=>Unit) =
    if(prevPairOpt.isEmpty) thenDo(set(currPair)) else diff(prevPairOpt.get, currPair)
}
