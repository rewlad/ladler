package ee.cone.base.lmdb

import ee.cone.base.connection_api.LifeCycle
import ee.cone.base.db.InstantEnvKey
import ee.cone.base.db_mix.DBAppMix
import ee.cone.base.lifecycle_mix.BaseConnectionMix

trait InstantLightningDBAppMix extends DBAppMix {
  lazy val instantDB =
    new LightningDBEnv[InstantEnvKey](0L, ".", 1L << 30, executionManager, createStorageConnection)
  lazy val createStorageConnection =
    (lifeCycle:LifeCycle) ⇒ new StorageConnectionMix(this, lifeCycle)
  override def toStart = instantDB :: super.toStart
}

class StorageConnectionMix(
  app: InstantLightningDBAppMix, val lifeCycle: LifeCycle
) extends BaseConnectionMix {
  lazy val lightningConnection = new LightningConnection(app.instantDB,lifeCycle)
}
