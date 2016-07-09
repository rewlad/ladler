package ee.cone.base.lmdb

import ee.cone.base.connection_api.LifeCycle
import ee.cone.base.db_impl.{InstantEnvKey, DBAppMix}
import ee.cone.base.lifecycle_impl.BaseConnectionMix

trait LightningDBAppMix extends DBAppMix {
  lazy val instantDB =
    new LightningDBEnv[InstantEnvKey](0L, ".", 1L << 30, executionManager, createStorageConnection)
  lazy val createStorageConnection =
    (lifeCycle:LifeCycle) ⇒ new StorageConnectionMix(this, lifeCycle)
  override def toStart = instantDB :: super.toStart
}

class StorageConnectionMix(
  app: LightningDBAppMix, val lifeCycle: LifeCycle
) extends BaseConnectionMix {
  lazy val lightningConnection = new LightningConnection(app.instantDB,lifeCycle)
}
