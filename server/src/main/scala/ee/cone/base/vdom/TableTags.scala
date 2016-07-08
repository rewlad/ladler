package ee.cone.base.vdom

import ee.cone.base.vdom.Types.VDomKey

trait ChildOfTable
trait ChildOfTableRow

trait TableTags {
  type CellContentVariant = Boolean
  def table(key: VDomKey, attr: TagAttr*)(children:List[ChildOfTable]): List[ChildPair[OfDiv]]
  def row(key: VDomKey, attr: TagAttr*)(children:List[ChildOfTableRow]): ChildOfTable
  def group(key:VDomKey, attr: TagAttr*)(children:List[ChildPair[OfDiv]]): ChildOfTableRow
  def cell(key:VDomKey, attr: TagAttr*)(children:CellContentVariant=>List[ChildPair[OfDiv]]): ChildOfTableRow
}

case object IsHeader extends TagAttr
case class MaxWidth(value:Int) extends TagAttr
case class MinWidth(value:Int) extends TagAttr
case class Divider(value:VDomValue) extends TagAttr