package io.github.rewlad

case class World(version: Int=0, hold: Option[Hold]=None, passiveCircles: List[Circle]=Nil){
  def r = 20
  def withManyCircles = copy(passiveCircles = (for(x←0 to 15; y←0 to 15) yield Circle(Point((1+x*2)*r,(1+y*2)*r),r)).toList)
  def beginHold(cursor: Point): World = if(hold.isEmpty){
    passiveCircles.find{ c ⇒ (c.center - cursor).r2 < c.r*c.r }.map{ circle ⇒
      copy(
        version = version + 1,
        passiveCircles = passiveCircles.filter(circle ne _),
        hold = Some(Hold(circle, cursor))
      )
    }.getOrElse(this)
  } else this
  def moveHold(cursor: Point): World = hold.map{ hold ⇒
    copy(
      version = version + 1,
      hold = Some(hold.move(cursor))
    )
  }.getOrElse(this)
  def endHold: World = hold.map{ hold ⇒
    copy(
      version = version + 1,
      passiveCircles = hold.circle :: passiveCircles,
      hold = None
    )
  }.getOrElse(this)
}

case class Point(x: Int, y: Int){
  def -(that: Point) = Point(x-that.x,y-that.y)
  def +(that: Point) = Point(x+that.x,y+that.y)
  def r2 = x*x+y*y
}
case class Circle(center: Point, r: Int)
case class Hold(circle: Circle, cursor: Point){
  def move(p: Point): Hold = copy(
    circle = circle.copy(center=circle.center-cursor+p),
    cursor = p
  )
}