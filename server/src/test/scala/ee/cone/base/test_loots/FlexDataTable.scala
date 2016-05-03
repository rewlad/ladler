package ee.cone.base.test_loots

import ee.cone.base.vdom.Types.VDomKey
import ee.cone.base.vdom.{OfDiv, ChildPair}


/**
  * Created by wregs on 5/3/2016.
  */

trait FlexDataTableElement{
  def elType:String
  var defaultRowHeight:Int =0
  def genView():List[ChildPair[OfDiv]]
}
case class FlexDataTableHeader(key:VDomKey,flexTags: FlexTags,flexDataTableElement: FlexDataTableElement*)
  extends FlexDataTableElement{

  def elType="header"
  def genView()={
    flexTags.divWrapper(key,None,None,None,None,None,None,
      flexDataTableElement.flatMap(x=>x.genView()).toList
    )::Nil
  }
}
case class FlexDataTableBody(flexDataTableElement: FlexDataTableElement*) extends FlexDataTableElement{

  def elType="body"
  def genView()=Nil
}
case class FlexDataTableRow(flexDataTableElement: FlexDataTableElement*) extends FlexDataTableElement{

  def elType="row"
  def genView()=Nil
}
case class FlexDataTableColGroup(flexDataTableElement: FlexDataTableElement*) extends FlexDataTableElement{

  def elType="group"
  def genView()=Nil
}
case class FlexDataTableCell(flexDataTableElement: FlexDataTableElement*) extends FlexDataTableElement{

  def elType="cell"
  def genView()=Nil
}
case class FlexDataTableControlPanel(key:VDomKey,flexTags: FlexTags,children:ChildPair[OfDiv]*) extends FlexDataTableElement{

  def elType="control"
  def genView()={

    flexTags.divWrapper(key,None,None,None,None,None,None,
      List(
        flexTags.divWrapper("1",Some("inline-block"),Some("1px"),Some("1px"),Some(s"${defaultRowHeight}px"),None,None,List()),
        flexTags.divWrapper("2",None,None,None,None,Some("right"),None,children.toList))
    )::Nil
  }
}
case class FlexDataTable(tableWidth:Float, flexDataTableElement: FlexDataTableElement*) {
  val _flexDataTableElement=flexDataTableElement.toList
  private def defaultRowHeight=48
  private def controlPanel={
    val cPanel=_flexDataTableElement.filter(x=>x.elType=="control")
    if(cPanel.nonEmpty){ cPanel.head.genView()}

    else Nil
  }
  private def header={
    val _header=_flexDataTableElement.filter(x=>x.elType=="header")
    _header.head.genView()
  }
  def genView()= {
    /*
    flexDataTableElement.foreach(x => {
      println(x.elType)
      x.genView()
    })*/
    controlPanel:::header
  }
  flexDataTableElement.foreach(x=>x.defaultRowHeight=defaultRowHeight)
}