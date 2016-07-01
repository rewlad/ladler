package ee.cone.base.test_loots

import ee.cone.base.connection_api.{CoHandler, CoHandlerProvider}
import ee.cone.base.db.{ItemListFactory, ItemListOrderingFactory, _}
import ee.cone.base.vdom.{ChildPair, CurrentView, OfDiv, ViewPath}

class UserListView(
  attrFactory: AttrFactory,
  findAttrs: FindAttrs,
  alienAttrs: AlienAccessAttrs,
  alien: Alien,
  filterObjFactory: FilterObjFactory,
  itemListFactory: ItemListFactory,
  itemListOrderingFactory: ItemListOrderingFactory,
  userAttrs: UserAttrs,
  users: Users,
  currentVDom: CurrentView,
  style: TagStyles,
  divTags: DivTags,
  htmlTable: TableTags,
  materialTags: MaterialTags,
  materialIconTags: MaterialIconTags,
  appUtils: AppUtils,
  tableUtils: MaterialDataTableUtils,
  fields: Fields
) extends CoHandlerProvider {
  import appUtils._
  import htmlTable._
  import materialTags._
  import materialIconTags._
  import tableUtils._
  import fields.field
  import divTags._


  def loginView(): List[ChildPair[OfDiv]] = {
    if(!users.needToLogIn) return Nil
    val showLabel = true

    val dialog = filterObjFactory.create(List(attrFactory.attrId(userAttrs.asActiveUser)))
    List(
      helmet("Login"),
      div("1", style.maxWidth(400))(List(
        paperWithMargin("login",
          div("1")(field(dialog, userAttrs.username, showLabel, IsPersonFieldOption)),
          div("2")(field(dialog, userAttrs.unEncryptedPassword, showLabel, DeferSendFieldOption(false), IsPasswordFieldOption)),
          div("3",style.alignRight,style.alignTop)(users.loginAction(dialog).map(btnRaised("login","LOGIN")(_)).toList)
        )
      ))
    )
  }

  private def view(pf: String) = wrapDBView { () =>
    val filterObj = filterObjFactory.create(List(attrFactory.attrId(userAttrs.asUser)))
    val userList = itemListFactory.create(users.findAll, users.world, filterObj, Nil, editable = true) //todo roles
  val itemListOrdering = itemListOrderingFactory.itemList(filterObj)
    val showPasswordCols = userList.list.exists(user=>user(alienAttrs.isEditing))//filters.editing(userAttrs.asUser)(nonEmpty) //todo: fix editing bug!!!
    List(
      toolbar("Users"),
      paperTable("table")(
        controlPanel(Nil, addRemoveControlView(userList)),
        row("head",List(IsHeader))(
          selectAllGroup(userList) :::
            List(
              group("2_grp", MinWidth(300)),
              cell("1",MinWidth(250))(_⇒sortingHeader(itemListOrdering,userAttrs.fullName)),
              cell("2",MinWidth(250))(_⇒sortingHeader(itemListOrdering,userAttrs.username)),
              cell("3",MinWidth(100),MaxWidth(150))(_⇒sortingHeader(itemListOrdering,userAttrs.asActiveUser))
            ) :::
            (if(showPasswordCols) List(
              group("3_grp",MinWidth(150)),
              cell("4",MinWidth(150))(_⇒sortingHeader(itemListOrdering,userAttrs.unEncryptedPassword)),
              cell("5",MinWidth(150))(_⇒sortingHeader(itemListOrdering,userAttrs.unEncryptedPasswordAgain)),
              cell("6",MinWidth(150))(_⇒Nil)
            ) else Nil) :::
            editAllGroup()
        ) ::
          userList.list.sorted(itemListOrdering.compose(creationTimeOrdering)).map{ user ⇒
            val srcId = user(alien.objIdStr)
            row(srcId,toggledSelectedRow(user))(
              selectRowGroup(user) :::
                List(
                  group("2_grp", MinWidth(300)),
                  cell("1",MinWidth(250))(showLabel⇒field(user, userAttrs.fullName, showLabel)),
                  cell("2",MinWidth(250))(showLabel⇒field(user, userAttrs.username, showLabel, IsPersonFieldOption)),
                  cell("3",MinWidth(100),MaxWidth(150))(showLabel⇒
                    if(user(userAttrs.asActiveUser)(findAttrs.nonEmpty)) List(materialChip("0","Active")(None)) else Nil
                  )
                ) :::
                (if(showPasswordCols) List(
                  group("3_grp",MinWidth(150)),
                  cell("4",MinWidth(150))(showLabel⇒field(user, userAttrs.unEncryptedPassword, showLabel, DeferSendFieldOption(false), IsPasswordFieldOption)),
                  cell("5",MinWidth(150))(showLabel⇒field(user, userAttrs.unEncryptedPasswordAgain, showLabel, DeferSendFieldOption(false), IsPasswordFieldOption)),
                  cell("6",MinWidth(150)) { _ =>
                    users.changePasswordAction(user).map(
                      btnRaised("doChange", "Change Password")(_)
                    ).toList
                  }
                ) else Nil) :::
                editRowGroup(userList, user)
            )
          }
      )
    )
  }
  def handlers = List(
    CoHandler(MenuItems)(()⇒List(
      option("users","Users")(()⇒currentVDom.relocate("/userList"))
    )),
    CoHandler(ViewPath("/userList"))(view)
  )
}

