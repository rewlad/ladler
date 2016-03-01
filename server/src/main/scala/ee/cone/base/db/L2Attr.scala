package ee.cone.base.db

import ee.cone.base.connection_api.ConnectionComponent
import ee.cone.base.util.Never

class AttrFactoryImpl(booleanConverter: RawValueConverter[Boolean], db: FactIndexImpl) extends AttrFactory {
  def apply[Value](labelId: Long, propId: Long, converter: RawValueConverter[Value]) = {
    val booleanAttr = AttrImpl[Boolean](labelId, propId)(db, booleanConverter, identity)
    AttrImpl(labelId, propId)(db, converter, _=>booleanAttr)
  }
}

case class AttrImpl[Value](labelId: Long, propId: Long)(
  val factIndex: FactIndexImpl, val converter: RawValueConverter[Value],
  getNonEmpty: Attr[Value]=>Attr[Boolean]
) extends Attr[Value] with RawAttr[Value] {
  def get(node: DBNode) = rawAttr.factIndex.get(node, rawAttr)
  def set(node: DBNode, value: Value) = rawAttr.factIndex.set(node, rawAttr, value)
  lazy val ref = new RefAttr(this)
  def nonEmpty: Attr[Boolean] = getNonEmpty(this)
  def rawAttr = this
}

case class RefAttr[Value](inner: Attr[Value]) extends Attr[Ref[Value]] {
  lazy val nonEmpty = TrueAttr(inner.nonEmpty.rawAttr)
  def set(node: DBNode, value: Ref[Value]) = Never()
  def get(node: DBNode) = RefImpl[Value](node, inner)
  def rawAttr = Never()
  lazy val ref = new RefAttr(this)
}

case class TrueAttr(rawAttr: RawAttr[Boolean]) extends Attr[Boolean] {
  def nonEmpty = this
  def set(node: DBNode, value: Boolean) = if(!value) Never()
  def get(node: DBNode) = true
  lazy val ref = new RefAttr(this)
}

case class RefImpl[Value](node: DBNode, attr: Attr[Value]) extends Ref[Value] {
  def apply() = attr.get(node)
  def update(value: Value) = attr.set(node, value)
}

class NodeHandlerListsImpl(components: =>List[ConnectionComponent]) extends NodeHandlerLists {
  def list[In,Out](ev: EventKey[In,Out]): List[CoHandler[In,Out]] =
    value.getOrElse(ev,Nil).asInstanceOf[List[CoHandler[In,Out]]]
  private lazy val value: Map[EventKey[_,_], List[ConnectionComponent]] =
    components.collect { case h: CoHandler[_,_] ⇒ h.on.map(ev=>(ev:EventKey[_,_],h:ConnectionComponent)) }
      .flatten.groupBy(_._1).mapValues(_.map(_._2))
}


