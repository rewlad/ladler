
////

case class PreventChangesIfAppliedAttrCalc(
  isAppliedAttr: RuledIndex,
  version: String = "e959c2f3-7c70-4e4e-aa3e-64516f613f39"
)(
  allAttrInfoList: ()=>List[AttrInfo]
) extends AttrCalc {
  override def recalculate(objId: ObjId) = if(isAppliedAttr(objId)!=DBRemoved) Never()
  override def affectedBy =
    allAttrInfoList().collect{ case i: RuledIndex if i != isAppliedAttr => i }
}
case class PreventUnsetAppliedAttrCalc(
  isAppliedAttr: RuledIndex,
  version: String = "bba34082-d0fd-4d16-b191-265b1fc06d21"
) extends AttrCalc {
  override def recalculate(objId: ObjId) = if(isAppliedAttr(objId)==DBRemoved) Never()
  override def affectedBy = isAppliedAttr :: Nil
}

class CachedLife(lifeCycle: ()=>LifeCycle) {
  def apply[C<:AutoCloseable](create: =>C): ()=>C = {
    var state: Option[C] = None
    () => state.getOrElse(
      lifeCycle().setup(Setup(create){ i => state = Option(i) })
        { i => state = None; i.close() }
    )
  }
}

class Cached[T](create: ()=>T){
  private var value: Option[T] = None
  def reset() = value = None
  def apply() = {
    if(value.isEmpty) value = Option(create())
    value.get
  }
  //def sub(create: =>T) = Setup(new Cached(()=>create))( it => subs = it :: subs)
}


