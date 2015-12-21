package io.github.rewlad

import org.scalastuff.json.JsonHandler

import scala.annotation.tailrec

/**
  * Created by skh on 21.12.15.
  */
class TestJsonHandler extends JsonHandler {
  object StartJSO
  var state: List[Object]  = Nil

  @tailrec private def endObject(r: Map[String,Object], l: List[Object]): List[Object] = l match {
    case h :: t if h eq StartJSO ⇒ r :: t
    case v :: k :: t ⇒ endObject(r+(k.asInstanceOf[String]→v), t)
  }
  @tailrec private def endArray(r: List[Object], l: List[Object]): List[Object] = l match {
    case h :: t if h eq StartJSO ⇒ r :: t
    case v :: t ⇒ endArray(v :: r, t)
  }

  def startObject() = state = StartJSO :: state
  def endObject()   = state = endObject(Map(),state)
  def startArray()  = state = StartJSO :: state
  def endArray()    = state = endArray(Nil,state)

  def string(s: String) = state = s :: state
  def number(n: String) = state = BigDecimal(n) :: state
  def falseValue(): Unit = ???
  def trueValue(): Unit = ???
  def nullValue(): Unit = ???

  def startMember(name: String) = state = name :: state
  def error(message: String, line: Int, pos: Int, excerpt: String): Unit = ???
}

/*
case object StartJSO

class TestJsonHandler extends JsonHandler {
  trait Ctx {
    def parent: Ctx
    def result: Object
    def add(v: Object): Unit
    def key_= (k: String): Unit
    def end() = { parent.add(result); parent }
  }
  class ArrayCtx(val parent: Ctx) extends Ctx {
    private var acc: List[Object] = Nil
    def result = acc.reverse
    def add(v: Object) = acc = v :: acc
    def key_= (k: String) = ???
  }
  class ObjectCtx(val parent: Ctx) extends Ctx {
    var result: Map[String,Object] = Map()
    var key = ""
    def add(v: Object) = result += key → v
  }
  object RootCtx extends Ctx {
    def parent = ???


  }

  var ctx: Ctx = RootCtx

  def startObject() = ctx = new ObjectCtx(ctx)
  def endObject()   = ctx = ctx.end()
  def startArray()  = ctx = new ArrayCtx(ctx)
  def endArray()    = ctx = ctx.end()

  def string(s: String): Unit = ctx.add(s)
  def number(n: String): Unit = ctx.add(BigDecimal(n))
  def falseValue(): Unit = ???
  def trueValue(): Unit = ???
  def nullValue(): Unit = ???

  def startMember(name: String) = ctx.key = name
  def error(message: String, line: Int, pos: Int, excerpt: String): Unit = ???
}
*/
