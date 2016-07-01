package ee.cone.base.test_loots

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

class Fields(
  handlerLists: CoHandlerLists,
  attrFactory: AttrFactory
) {
  def field[Value](obj: Obj, attr: Attr[Value], showLabel: Boolean, options: FieldOption*): List[ChildPair[OfDiv]] =
    handlerLists.single(ViewField(attrFactory.valueType(attr)), ()⇒Never())(obj,attr,showLabel,options)
}
class FieldAttributesImpl(
  findAttrs: FindAttrs,
  validationAttrs: ValidationAttributes,
  alienAttrs: AlienAccessAttrs
) extends FieldAttributes {
  def aNonEmpty: Attr[Boolean] = findAttrs.nonEmpty
  def aValidation: Attr[ObjValidation] = validationAttrs.validation
  def aIsEditing: Attr[Boolean] = alienAttrs.isEditing
  def aObjIdStr: Attr[String] = alienAttrs.objIdStr
}

class AppUtils(
  handlerLists: CoHandlerLists,
  errorListView: ErrorListView,
  userListView: UserListView,
  currentVDom: CurrentView,
  materialTags: MaterialTags
) extends CoHandlerProvider {
  import materialTags._

  def eventSource = handlerLists.single(SessionEventSource, ()⇒Never())

  private def emptyView(pf: String) =
    root(List(text("text", "Loading...")))
  def wrapDBView(view: ()=>List[ChildPair[OfDiv]]): VDomValue =
    root(eventSource.incrementalApplyAndView { () ⇒
      errorListView.errorNotification ::: {
        val dialog = userListView.loginView()
        if(dialog.nonEmpty) dialog else withAdaptiveInvalidationPeriod(view)
      }
    })

  def withAdaptiveInvalidationPeriod(view: ()=>List[ChildPair[OfDiv]]) = {
    val startTime = System.currentTimeMillis
    val res = view()
    val endTime = System.currentTimeMillis
    currentVDom.until(endTime + (endTime - startTime) * 10)
    res
  }

  def handlers = List(
    CoHandler(ViewPath(""))(emptyView),
    CoHandler(SessionInstantAdded)(currentVDom.invalidate),
    CoHandler(TransientChanged)(currentVDom.invalidate)
  )
}
