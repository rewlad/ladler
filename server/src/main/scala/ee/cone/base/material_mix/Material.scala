package ee.cone.base.material_mix

import ee.cone.base.material_impl._
import ee.cone.base.connection_api.{BasicValueTypes, FieldAttributes}
import ee.cone.base.vdom.{DBRootWrap, TableTags}
import ee.cone.base.vdom_mix.VDomConnectionMix

trait MaterialConnectionMix extends VDomConnectionMix {
  def dbRootWrap: DBRootWrap
  def fieldAttributes: FieldAttributes
  def wrappedByMaterialTableTags: TableTags
  def basicValueTypes: BasicValueTypes

  lazy val materialStyles = new MaterialStylesImpl
  lazy val tableUtilTags = new TableUtilTagsImpl(tags,tagStyles,materialStyles)
  lazy val buttonTags = new ButtonTagsImpl(childPairFactory, tagJsonUtils, tags, tagStyles)
  lazy val optionTags = new OptionTagsImpl(childPairFactory, tags, tagStyles)
  lazy val materialTags = new MaterialTagsImpl(
    handlerLists, dbRootWrap, childPairFactory, tags, tagStyles,
    optionTags, buttonTags, materialStyles
  )
  lazy val materialFields = new MaterialFields(
    fieldAttributes,handlerLists,childPairFactory, tagJsonUtils, tags, tagStyles,
    optionTags, buttonTags, basicValueTypes
  )
  lazy val materialTableTags = new MaterialTableTags(wrappedByMaterialTableTags,tags,tagStyles,materialStyles)
  lazy val tableIconTags = new TableIconTagsImpl
  lazy val eventIconTagsImpl = new EventIconTagsImpl
}
