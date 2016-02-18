package ee.cone.base.db

import ee.cone.base.connection_api.ConnectionComponent
import ee.cone.base.db.Types._
import ee.cone.base.util.Never

import scala.collection.mutable

class DBNodeImpl(val objId: Long) extends DBNode {
  def apply[Value](attr: Prop[Value]) = attr.get(this)
  def update[Value](attr: Prop[Value], value: Value) = attr.set(this,value)
}

class NodeValueConverter extends DBValueConverter[Option[DBNode]] {
  def apply(value: Option[DBNode]): DBValue = value match {
    case None => DBRemoved
    case Some(node) => DBLongValue(node.objId)
  }
  def apply(value: DBValue): Option[DBNode] = value match {
    case DBRemoved => None
    case DBLongValue(v) => Some(new DBNodeImpl(v))
    case _ => Never()
  }
}

class BooleanValueConverter extends DBValueConverter[Boolean] {
  def apply(value: Boolean): DBValue = if(value) DBLongValue(1L) else DBRemoved
  def apply(value: DBValue): Boolean = value != DBRemoved
}

class ListByDBNode(inner: FactIndex, index: AttrId=>Prop[_]) extends Prop[List[Prop[_]]] {
  def get(node: DBNode) = {
    val feed = new ListFeedImpl[AttrId,Prop[_]](index)
    inner.execute(node.objId, feed)
    feed.result
  }
  def set(node: DBNode, value: List[Prop[_]]) = Never()
  def converter = Never()
  def attrId = Never()
  def nonEmpty = Never() //?
  def components = Nil
}

case class ListByValueImpl[Value](attrCalc: SearchAttrCalc)(
  searchIndex: SearchIndex, converter: DBValueConverter[Value]
) extends ListByValue[Value] {
  def attrId = attrCalc.searchAttrId
  def list(value: Value): List[DBNode] = {
    val feed = new ListFeedImpl[ObjId,DBNode](objId=>new DBNodeImpl(objId))
    searchIndex.execute(attrId, converter(value), feed)
    feed.result.reverse
  }
  def list(value: Value, fromNode: DBNode): List[DBNode] = {
    val feed = new ListFeedImpl[ObjId,DBNode](objId=>new DBNodeImpl(objId))
    searchIndex.execute(attrId, converter(value), fromNode.objId, feed)
    feed.result.reverse
  }
  def components = attrCalc :: Nil
}

class ListFeedImpl[From,To](converter: From=>To) extends Feed[From] {
  var result: List[To] = Nil
  override def apply(value: From) = {
    result = converter(value) :: result
    true
  }
}

case class PreCommitCheckAttrCalcImpl(check: PreCommitCheck) extends PreCommitCheckAttrCalc {
  private lazy val objIds = mutable.SortedSet[ObjId]()
  def affectedBy = check.affectedBy.map(_.attrId)
  def beforeUpdate(objId: ObjId) = ()
  def afterUpdate(objId: ObjId) = objIds += objId
  def checkAll() = check.check(objIds.toSeq.map(new DBNodeImpl(_)))
}

class CheckAll(components: =>List[ConnectionComponent]) {
  def apply(): Seq[ValidationFailure] =
    components.collect{ case i: PreCommitCheckAttrCalc => i.checkAll() }.flatten
}

case class PropImpl[Value](attrId: AttrId, nonEmpty: Prop[Boolean], components: List[AttrCalc])(
  db: FactIndex, converter: DBValueConverter[Value]
) extends Prop[Value] {
  def set(node: DBNode, value: Value) = db.set(node.objId, attrId, converter(value))
  def get(node: DBNode) = converter(db.get(node.objId, attrId))
}

class AttrCalcAdapter(inner: NodeAttrCalc) extends AttrCalc {
  def affectedBy = inner.affectedBy.map(_.attrId)
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