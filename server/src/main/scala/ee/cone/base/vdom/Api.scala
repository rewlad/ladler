
package ee.cone.base.vdom

//import ee.cone.base.connection_api.EventKey
import ee.cone.base.vdom.Types._

trait ToJson {
  def appendJson(builder: JsonBuilder): Unit
}
trait VDomValue extends ToJson

////

trait JsonBuilder {
  def startArray(): JsonBuilder
  def startObject(): JsonBuilder
  def end(): JsonBuilder
  def append(value: String): JsonBuilder
  def append(value: Boolean): JsonBuilder
}

////

object Types {
  type VDomKey = String
}

trait ChildPair[C] {
  def key: VDomKey
}

trait ChildPairFactory {
  def apply[C](key: VDomKey, theElement: VDomValue, elements: List[ChildPair[_]]): ChildPair[C]
}

////

abstract class TagName(val name: String)

trait TagAttr
trait TagStyle extends TagAttr {
  def appendStyle(builder: JsonBuilder): Unit
}

trait Color {
  def value: String
}

////

trait CurrentView {
  def invalidate(): Unit
  def until(value: Long): Unit
  def relocate(value: String): Unit
}

trait OnClickReceiver {
  def onClick: Option[()⇒Unit]
}

trait OnChangeReceiver {
  def onChange: Option[String⇒Unit]
}

trait OnResizeReceiver{
  def onResize: Option[String⇒Unit]
}

////

trait TagJsonUtils {
  def appendInputAttributes(builder: JsonBuilder, value: String, deferSend: Boolean): Unit
}
