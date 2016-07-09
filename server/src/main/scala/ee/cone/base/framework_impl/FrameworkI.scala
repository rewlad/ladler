package ee.cone.base.framework_impl

import ee.cone.base.connection_api.{Attr, Obj, ObjId}
import ee.cone.base.db.{SearchByLabelProp}

import ee.cone.base.framework.Users
import ee.cone.base.vdom._

trait UserListView {
  def loginView: List[ChildPair[OfDiv]]
}

trait UserAttributes {
  def asUser: Attr[Obj]
  def fullName: Attr[String]
  def username: Attr[String]
  def unEncryptedPassword: Attr[String]
  def unEncryptedPasswordAgain: Attr[String]
  def asActiveUser: Attr[Obj]
  def authenticatedUser: Attr[Obj]
  def world: ObjId
}

trait UsersI extends Users {
  def needToLogIn: Boolean
  def findAll: SearchByLabelProp[Obj]
  def loginAction(dialog: Obj): Option[()⇒Unit]
  def changePasswordAction(user: Obj): Option[()⇒Unit]
}
