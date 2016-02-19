package ee.cone.base.db

import ee.cone.base.connection_api.ConnectionComponent
import ee.cone.base.db.Types._
import ee.cone.base.util.Never

import scala.collection.mutable

case class DBNodeImpl(val objId: Long) extends DBNode {
  def apply[Value](attr: Prop[Value]) = attr.get(this)
  def update[Value](attr: Prop[Value], value: Value) = attr.set(this,value)
}

class NodeValueConverter(inner: InnerRawValueConverter) extends RawValueConverter[Option[DBNode]] {
  def convert() = None
  def convert(value: Long) = Some(new DBNodeImpl(value))
  def convert(valueA: Long, valueB: Long) = Never()
  def convert(value: String) = Never()
  def allocWrite(before: Int, value: Option[DBNode], after: Int) =
    if(nonEmpty(value)) inner.allocWrite(before, value.get.objId, after) else Never()
  def nonEmpty(value: Option[DBNode]) = value.nonEmpty
  def same(valueA: Option[DBNode], valueB: Option[DBNode]) =
    valueA.map(_.objId) == valueB.map(_.objId)
}

class BooleanValueConverter(inner: InnerRawValueConverter) extends RawValueConverter[Boolean] {
  def convert() = false
  def convert(value: Long) = true
  def convert(valueA: Long, valueB: Long) = true
  def convert(value: String) = true
  def allocWrite(before: Int, value: Boolean, after: Int) =
    if(nonEmpty(value)) inner.allocWrite(before, 1L, after) else Never()
  def nonEmpty(value: Boolean) = value
  def same(valueA: Boolean, valueB: Boolean) = valueA == valueB
}

class ListByDBNode(inner: FactIndex, index: (Long,Long)=>Prop[_]) extends Prop[List[Prop[_]]] {
  def get(node: DBNode) = {
    val feed = new ListFeedImpl[Prop[_]](index)
    inner.execute(node.objId, feed)
    feed.result
  }
  def set(node: DBNode, value: List[Prop[_]]) = Never()
  def converter = Never()
  def attrId = Never()
  def nonEmpty = Never() //?
  def components = Nil
}

case class ListByValueImpl[Value](attrCalc: SearchAttrCalc[Value])(
  searchIndex: SearchIndex
) extends ListByValue[Value] {
  def attrId = attrCalc.searchAttrId
  def list(value: Value): List[DBNode] = {
    val feed = new ListFeedImpl[DBNode]((objId,_)=>new DBNodeImpl(objId))
    searchIndex.execute(attrId, value, feed)
    feed.result.reverse
  }
  def list(value: Value, fromNode: DBNode): List[DBNode] = {
    val feed = new ListFeedImpl[DBNode]((objId,_)=>new DBNodeImpl(objId))
    searchIndex.execute(attrId, value, fromNode.objId, feed)
    feed.result.reverse
  }
  def components = attrCalc :: Nil
}

class ListFeedImpl[To](converter: (Long,Long)=>To) extends Feed {
  var result: List[To] = Nil
  def apply(valueA: Long, valueB: Long) = {
    result = converter(valueA,valueB) :: result
    true
  }
}

case class PreCommitCheckAttrCalcImpl(check: PreCommitCheck) extends PreCommitCheckAttrCalc {
  private lazy val objIds = mutable.SortedSet[ObjId]()
  def affectedBy = check.affectedBy.map(_.attrId.nonEmpty)
  def beforeUpdate(objId: ObjId) = ()
  def afterUpdate(objId: ObjId) = objIds += objId
  def checkAll() = check.check(objIds.toSeq.map(new DBNodeImpl(_)))
}

class CheckAll(components: =>List[ConnectionComponent]) {
  def apply(): Seq[ValidationFailure] =
    components.collect{ case i: PreCommitCheckAttrCalc => i.checkAll() }.flatten
}

case class PropImpl[Value](attrId: AttrId[Value], nonEmpty: Prop[Boolean], components: List[AttrCalc])(
  db: FactIndex
) extends Prop[Value] {
  def set(node: DBNode, value: Value) = db.set(node.objId, attrId, value)
  def get(node: DBNode) = db.get(node.objId, attrId)
}

class AttrCalcAdapter(inner: NodeAttrCalc) extends AttrCalc {
  def affectedBy = inner.affectedBy.map(_.attrId.nonEmpty)
  def beforeUpdate(objId: ObjId) = inner.beforeUpdate(new DBNodeImpl(objId))
  def afterUpdate(objId: ObjId) = inner.afterUpdate(new DBNodeImpl(objId))
}

//lazy val keyForValue: String = propOpt.orElse(labelOpt).flatMap(_.nameOpt).get




















/*
  lazy val version = MD5(Bytes(attrInfoList.collect {
    //case i: RuledIndex if i.indexed ⇒
      //println(s"ai: ${i.attrId.toString}")
    //  i.attrId.toString
    case i: AttrCalc ⇒
      //println(s"acc:${i.version}:$i")
      i.toString
  }.sorted.mkString(",")))*/


/*
case class ExtractedFact(objId: Long, attrId: Long, value: DBValue)
class Replay(db: IndexingTx) {
  private lazy val changedOriginalSet = mutable.SortedSet[(Long,Long)]()
  def set(facts: List[ExtractedFact]): Unit =
    facts.foreach(fact⇒set(fact.objId, fact.attrId, fact.value))
  def set(objId: Long, attrId: Long, value: DBValue): Unit =
    if(db.set(objId, attrId, value, db.isOriginal))
      changedOriginalSet += ((objId,attrId))
  def changedOriginalFacts: List[ExtractedFact] = changedOriginalSet.map{
    case (objId, attrId) ⇒ ExtractedFact(objId, attrId, db(objId, attrId))
  }.toList
}
*/