package ee.cone.base.framework_impl

import ee.cone.base.db_impl.DBConnectionMix
import ee.cone.base.flexlayout_impl.FlexConnectionMix
import ee.cone.base.framework.ErrorListView
import ee.cone.base.lifecycle_impl.BaseConnectionMix
import ee.cone.base.material_impl.MaterialConnectionMix

trait FrameworkConnectionMix extends BaseConnectionMix with DBConnectionMix with MaterialConnectionMix with FlexConnectionMix {
  def wrappedByMaterialTableTags = flexTableTags
  def tableTags = materialTableTags

  lazy val fieldAttributes = new FieldAttributesImpl(findAttrs,validationAttributes,alienAttributes)
  lazy val fields = new FieldsImpl(handlerLists,attrFactory)
  lazy val dataTableUtils = new DataTableUtilsImpl(
    handlerLists,objOrderingFactory,objSelectionAttributes,editing,
    objSelectionFactory,alien,alienAttributes,fieldAttributes,
    tagStyles,tags,buttonTags,materialTags,flexTags,tableTags,tableIconTags,fields
  )
  lazy val measure = new MeasureImpl
  def errorListView: ErrorListView
  lazy val dbRootWrap = new DBRootWrapImpl(handlerLists,errorListView,userListView,currentView,tags,measure)
  lazy val userAttributes = new UserAttributesImpl(attrFactory, labelFactory, objIdFactory, basicValueTypes)()
  lazy val users = new UsersImpl(
    userAttributes, nodeAttrs, fieldAttributes,
    handlerLists, factIndex, searchIndex, findNodes, mainTx, alien, transient,
    mandatory, unique, onUpdate, uiStrings
  )()
  lazy val userListView = new UserListViewImpl(
    attrFactory, filterObjFactory, indexedObjCollectionFactory, itemListOrderingFactory,
    userAttributes, users,
    currentView, tagStyles, tags, tableTags, optionTags, buttonTags, materialTags,
    dataTableUtils, fields, fieldAttributes, tableUtilTags
  )
}
