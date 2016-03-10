package ee.cone.base.db

import ee.cone.base.connection_api.{Obj, Attr}

// fail'll probably do nothing in case of outdated rel type

class RelTypeImpl(
  relStartSide: Attr[Obj],
  relEndSide: Attr[Obj],
  searchIndex: SearchIndex
) extends RelType {
  def apply(label: Attr[_]) =
    searchIndex.handlers(label, relStartSide) :::
    searchIndex.handlers(label, relEndSide)
    //add rel event
}
