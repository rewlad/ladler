
package ee.cone.base.framework_impl

import ee.cone.base.connection_api._
import ee.cone.base.db._
import ee.cone.base.server.SenderOfConnection
import ee.cone.base.util.Never
import ee.cone.base.vdom._

class FailOfConnection(
  sender: SenderOfConnection
) extends CoHandlerProvider {
  def handlers = CoHandler(FailEventKey){ e =>
    println(s"error: ${e.toString}")
    sender.sendToAlien("fail",e.toString) //todo
  } :: Nil
}

class FieldsImpl(
  handlerLists: CoHandlerLists,
  attrFactory: AttrFactory
) extends Fields {
  def field[Value](obj: Obj, attr: Attr[Value], showLabel: Boolean, options: FieldOption*) =
    handlerLists.single(ViewField(attrFactory.valueType(attr)), ()⇒Never())(obj,attr,showLabel,options)
}

class FieldAttributesImpl(
  findAttrs: FindAttrs,
  validationAttrs: ValidationAttributes,
  alienAttrs: AlienAttributes
) extends FieldAttributes {
  def aNonEmpty: Attr[Boolean] = findAttrs.nonEmpty
  def aValidation: Attr[ObjValidation] = validationAttrs.validation
  def aIsEditing: Attr[Boolean] = alienAttrs.isEditing
  def aObjIdStr: Attr[String] = alienAttrs.objIdStr
}

class DBRootWrapImpl(
  handlerLists: CoHandlerLists,
  errorListView: ErrorListView,
  userListView: UserListView,
  currentVDom: CurrentView,
  tags: Tags,
  measure: Measure
) extends DBRootWrap with CoHandlerProvider {
  private def eventSource = handlerLists.single(SessionEventSource, ()⇒Never())
  private def emptyView(pf: String) =
    List(tags.text("text", "Loading..."))
  def wrap(view: ()=>List[ChildPair[OfDiv]]) =
    eventSource.incrementalApplyAndView { () ⇒
      errorListView.errorNotification ::: {
        val dialog = userListView.loginView
        if(dialog.nonEmpty) dialog else measure(view)(
          (startTime: Long, endTime: Long) ⇒
            currentVDom.until(endTime + (endTime - startTime) * 10)
        )
      }
    }

  def handlers = List(
    CoHandler(ViewPath(""))(emptyView),
    CoHandler(SessionInstantAdded)(currentVDom.invalidate),
    CoHandler(TransientChanged)(currentVDom.invalidate)
  )
}
