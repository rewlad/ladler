package ee.cone.base.db

import ee.cone.base.connection_api._

trait ValidationContext { def wrap(obj: Obj): Obj }

class ValidationAttributesImpl(
  attr: AttrFactory,
  asObjValidation: AttrValueType[ObjValidation]
)(
  val validation: Attr[ObjValidation] = attr("b6b7ef56-a0e2-4c1b-982e-76254c42df9b",asObjValidation)
) extends ValidationAttributes

class ValidationWrapType extends WrapType[ObjValidation]

class ValidationFactoryImpl(
  at: ValidationAttributes,
  nodeAttrs: NodeAttrs, attrFactory: AttrFactory,
  dbWrapType: WrapType[ObjId],
  validationWrapType: WrapType[ObjValidation],
  uiStrings: UIStrings
)(
  val noObjValidation: ObjValidation = new ObjValidation {
    def get[Value](attr: Attr[Value]) = Nil
  }
) extends ValidationFactory with CoHandlerProvider {
  def context(stateList: List[ValidationState]) = {
    val stateMap = stateList.groupBy(_.objId).mapValues { states ⇒
      val values = states.groupBy(_.attrId)
      new ObjValidation {
        def get[Value](attr: Attr[Value]) =
          values.getOrElse(attrFactory.attrId(attr), Nil)
      }
    }
    new ValidationContext {
      def wrap(obj: Obj) =
        stateMap.get(obj(nodeAttrs.objId)).map(obj.wrap(validationWrapType,_))
          .getOrElse(obj)
    }
  }
  def need[Value](obj: Obj, attr: Attr[Value], check: Value⇒Option[String]) =
    check(obj(attr)).map{ text ⇒
      ValidationState(obj(nodeAttrs.objId), attrFactory.attrId(attr), text.nonEmpty, s"${uiStrings.caption(attr)} is required $text")
    }.toList

  def handlers =
    CoHandler(GetValue(validationWrapType,at.validation))( (obj,innerObj) ⇒ innerObj.data ) ::
    attrFactory.handlers(at.validation)( (obj,objId) ⇒ noObjValidation )
}
