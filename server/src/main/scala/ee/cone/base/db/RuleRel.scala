package ee.cone.base.db

// fail'll probably do nothing in case of outdated rel type

class RelTypeImpl(
  relStartSide: Attr[DBNode],
  relEndSide: Attr[DBNode],
  searchIndex: SearchIndex
) extends RelType {
  def apply(label: Attr[_]) =
    searchIndex.handlers(label, relStartSide) :::
    searchIndex.handlers(label, relEndSide)
    //add rel event
}
