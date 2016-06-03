package ee.cone.base.test_loots

import ee.cone.base.connection_api._
import ee.cone.base.db._

trait ValidationContext { def wrap(obj: Obj): Obj }
trait ValidationState
trait ObjValidation {
  def get[Value](attr: Attr[Value]): List[ValidationState]
}

class ValidationAttributes(
  attr: AttrFactory,
  asObjValidation: AttrValueType[ObjValidation]
)(
  val validation: Attr[ObjValidation] = attr("b6b7ef56-a0e2-4c1b-982e-76254c42df9b",asObjValidation)
)

trait ObjAttrState[Value] {
  def obj: Obj
  def attr: Attr[Value]
}
trait TextValidationState {
  def text: String
}

class ValidationWrapType extends WrapType[ObjValidation]

class ValidationFactory(
  at: ValidationAttributes,
  nodeAttrs: NodeAttrs, attrFactory: AttrFactory,
  dbWrapType: WrapType[ObjId],
  validationWrapType: WrapType[ObjValidation]
)(
  val noObjValidation: ObjValidation = new ObjValidation {
    def get[Value](attr: Attr[Value]) = Nil
  }
) extends CoHandlerProvider {
  def context(stateList: List[ValidationState]): ValidationContext = {
    val stateMap = stateList.collect{ case st: ObjAttrState[_] ⇒ st }
      .groupBy(_.obj(nodeAttrs.objId))
      .mapValues { states ⇒
        val values = states.groupBy(st ⇒ attrFactory.attrId(st.attr))
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
  def need[Value](objA: Obj, attrA: Attr[Value], check: Value⇒Option[String]) =
    check(objA(attrA)).map(textA⇒new ValidationState with ObjAttrState[Value] with TextValidationState {
      def obj = objA
      def attr = attrA
      def text = textA
    }).toList

  def handlers = List(
    CoHandler(GetValue(dbWrapType,at.validation))( (obj,innerObj) ⇒ noObjValidation ),
    CoHandler(GetValue(validationWrapType,at.validation))( (obj,innerObj) ⇒ innerObj.data )
  )
}
