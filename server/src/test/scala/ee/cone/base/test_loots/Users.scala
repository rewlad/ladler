
package ee.cone.base.test_loots // to app

import java.nio.ByteBuffer
import java.util.UUID

import ee.cone.base.connection_api._
import ee.cone.base.db._
import ee.cone.base.util.{Bytes, Never}


class UserAttrs(
  attr: AttrFactory,
  label: LabelFactory,
  objIdFactory: ObjIdFactory,
  asDBObj: AttrValueType[Obj],
  asString: AttrValueType[String],
  asUUID: AttrValueType[Option[UUID]]
)(
  val asUser: Attr[Obj] = label("f8c8d6da-0942-40aa-9005-261e63498973"),
  val fullName: Attr[String] = attr("a4260856-0904-40c4-a18a-6d925abe5044",asString),
  val username: Attr[String] = attr("4f0d01f8-a1a3-4551-9d07-4324d4d0e633",asString),
  val encryptedPassword: Attr[Option[UUID]] = attr("3a345f93-18ab-4137-bdde-f0df77161b5f",asUUID),
  val unEncryptedPassword: Attr[String] = attr("7d12edd9-a162-4305-8a0c-31ef3f2e3300",asString),
  val unEncryptedPasswordAgain: Attr[String] = attr("24517821-c606-4f6c-8e93-4f01c2490747",asString),
  val asActiveUser: Attr[Obj] = label("eac3b82c-5bf0-4278-8e0a-e1e0e3a95ffc"),
  val authenticatedUser: Attr[Obj] = attr("47ee2460-b170-4213-9d56-a8fe0f7bc1f5",asDBObj), //of session
  val world: ObjId = objIdFactory.toObjId("4c766f02-5a76-47c3-aec7-2315caa5828b"),
  val location: Attr[Obj] = attr("98e4646e-e5ba-4ac3-a2e2-4f79dde3886e",asDBObj)
)

class Users(
  at: UserAttrs, nodeAttrs: NodeAttrs, fieldAttributes: FieldAttributes,
  handlerLists: CoHandlerLists,
  factIndex: FactIndex, searchIndex: SearchIndex,
  findNodes: FindNodes, mainTx: CurrentTx[MainEnvKey],
  alien: Alien, transient: Transient, mandatory: Mandatory, unique: Unique,
  onUpdate: OnUpdate, captions: UIStrings,
  orderingFactory: ItemListOrderingFactory
)(
  val findAll: SearchByLabelProp[Obj] = searchIndex.create(at.asUser, at.location),
  val findAllActive: SearchByLabelProp[Obj] = searchIndex.create(at.asActiveUser, at.location),
  val findActiveByName: SearchByLabelProp[String] = searchIndex.create(at.asActiveUser, at.username)
) extends CoHandlerProvider {
  private def eventSource = handlerLists.single(SessionEventSource, ()⇒Never())
  private def encryptPassword(objId: ObjId, username: String, pw: String): UUID = {
    val buffer = ByteBuffer.allocate(256)
    buffer.putLong(objId.hi).putLong(objId.lo).put(Bytes(username)).put(Bytes(pw))
    UUID.nameUUIDFromBytes(buffer.array())
  }
  def changePasswordAction(user: Obj): Option[()⇒Unit] = {
    val userId = user(nodeAttrs.objId)
    val username = user(at.username)
    val pw = user(at.unEncryptedPassword)
    if(pw.nonEmpty && pw == user(at.unEncryptedPasswordAgain)) Some{()⇒
      user(at.encryptedPassword) = Some(encryptPassword(userId,username,pw))
      user(at.unEncryptedPassword) = ""
      user(at.unEncryptedPasswordAgain) = ""
      user(fieldAttributes.aIsEditing) = false
    }
    else None
  }
  def loginAction(dialog: Obj): Option[()⇒Unit] = {
    val username = dialog(at.username)
    val pw = dialog(at.unEncryptedPassword)
    if(username.isEmpty || pw.isEmpty) None else {
      val user = findNodes.single(findNodes.where(mainTx(), findActiveByName, dialog(at.username), Nil))
      val userId = user(nodeAttrs.objId)
      val mainSession = alien.wrapForUpdate(eventSource.mainSession)
      val encryptedPassword = if(userId.nonEmpty) user(at.encryptedPassword) else None
      Some{ () ⇒
        if(encryptedPassword.contains(encryptPassword(userId, username, pw)))
          mainSession(at.authenticatedUser) = user
        else throw new Exception("Bad username or password")
      }
    }
  }
  def world: Obj = findNodes.whereObjId(at.world)
  def needToLogIn: Boolean =
    !eventSource.mainSession(at.authenticatedUser)(at.asUser)(fieldAttributes.aNonEmpty) &&
      findNodes.where(mainTx(), findAllActive, world, FindFirstOnly::Nil).nonEmpty
  private def calcCanLogin(on: Boolean, user: Obj) =
    user(at.asActiveUser) = if(on) user else findNodes.noNode

  def handlers =
    CoHandler(ObjIdCaption(at.world))("Our World") ::
      CoHandler(AttrCaption(at.asUser))("User") ::
      CoHandler(AttrCaption(at.fullName))("Full Name") ::
      CoHandler(AttrCaption(at.username))("Username") ::
      CoHandler(AttrCaption(at.asActiveUser))("Active") ::
      CoHandler(AttrCaption(at.unEncryptedPassword))("Password") ::
      CoHandler(AttrCaption(at.unEncryptedPasswordAgain))("Repeat Password") ::
      CoHandler(AttrCaption(at.location))("Realm") ::
      CoHandler(AttrCaption(at.encryptedPassword))("Encrypted Password") ::
      List(findAll,findAllActive,findActiveByName).flatMap(searchIndex.handlers(_)) :::
      List(at.unEncryptedPassword, at.unEncryptedPasswordAgain).flatMap(transient.update) :::
      List(at.asUser,at.fullName,at.username,at.encryptedPassword,at.authenticatedUser,at.location).flatMap(alien.update(_)) :::
      List(at.asActiveUser).flatMap(factIndex.handlers) :::
      mandatory(at.location, at.username, mutual = false) :::
      mandatory(at.location, at.fullName, mutual = false) :::
      unique(at.asUser, at.username) :::
      unique(at.asUser, at.fullName) :::
      onUpdate.handlers(List(at.asUser, at.location, at.username, at.encryptedPassword), Nil)(calcCanLogin) :::
      captions.captions(at.asUser, at.fullName::Nil)(_(at.fullName))
}

