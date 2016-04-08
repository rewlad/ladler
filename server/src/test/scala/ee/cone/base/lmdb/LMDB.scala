package ee.cone.base.lmdb


import java.nio.ByteBuffer
import ee.cone.base.connection_api.{ExecutionManager, LifeCycle}
import ee.cone.base.db.Types.{RawValue, RawKey}
import ee.cone.base.db._
import ee.cone.base.util._
import org.fusesource.lmdbjni._

// PATH=/web/_data/jdk/bin:$PATH sbt 'show test:full-classpath'

class LightningDBEnv[DBEnvKey](
  val dbId: Long,
  path: String,
  mapSize: Long,
  lifeCycleManager: ExecutionManager
) extends DBEnv[DBEnvKey] {
  var state: Option[(Env,Database)] = None
  def start() = lifeCycleManager.startServer{ ()⇒
    println("A")
    try{
      val env = new Env()
      try {
        println("B")
        env.setMapSize(mapSize)
        env.open(path) // needs path dir exists here
        val db = env.openDatabase()
        try {
          synchronized{ state = Some((env,db)) }
          Thread.sleep(java.lang.Long.MAX_VALUE)
        } finally env.close()
      } finally env.close()
    } catch {
      case e: Throwable ⇒ println(e)
    }

  }

  def roTx(txLifeCycle: LifeCycle) = createTx(txLifeCycle, rw=false)
  def rwTx[R](txLifeCycle: LifeCycle)(f: (RawIndex) ⇒ R) = {
    val index = createTx(txLifeCycle, rw=true)
    val res = f(index)
    index.commit()
    res
  }
  private def maxValSize = 4096
  private def createTx(txLifeCycle: LifeCycle, rw: Boolean): LightningMergedIndex[DBEnvKey] = {
    val (env,db) = synchronized { state.get }
    val tx = Option(env.createTransaction(null, !rw)).get
    txLifeCycle.onClose(()⇒tx.close())
    val cursor = db.openCursor(tx)
    txLifeCycle.onClose(()⇒cursor.close())
    val keyByteBuffer = ByteBuffer.allocateDirect(maxValSize)
    val valByteBuffer = ByteBuffer.allocateDirect(maxValSize)
    val keyDirectBuffer = new DirectBuffer(keyByteBuffer)
    val valDirectBuffer = new DirectBuffer(valByteBuffer)

    new LightningMergedIndex(this, tx, cursor, keyByteBuffer, valByteBuffer, keyDirectBuffer, valDirectBuffer)
  }
}

class LightningMergedIndex[DBEnvKey](
  env: LightningDBEnv[DBEnvKey], tx: Transaction, cursor: Cursor,
  keyByteBuffer: ByteBuffer, valByteBuffer: ByteBuffer,
  keyDirectBuffer: DirectBuffer, valDirectBuffer: DirectBuffer
) extends RawIndex {
  var peek: SeekStatus = NotFoundStatus
  def commit() = {
    cursor.close()
    tx.commit()
  }

  private def bytesFromDirectBuffer(buffer: DirectBuffer): Array[Byte] = {
    val res = new Array[Byte](buffer.capacity)
    buffer.getBytes(0, res)
    res
  }
  private def bytesToDirectBuffer(byteBuffer: ByteBuffer, directBuffer: DirectBuffer, bytes: Array[Byte]): Unit = {
    keyByteBuffer.clear()
    keyByteBuffer.put(bytes)
    keyByteBuffer.flip()
    keyDirectBuffer.wrap(keyByteBuffer.slice())
  }

  private def prepareKey(key: Array[Byte]): Unit = {
    peek = NotFoundStatus
    bytesToDirectBuffer(keyByteBuffer, keyDirectBuffer, key)
  }
  def get(key: RawKey): RawValue = {
    //log  = new GetLogItem(this,key) :: log
    prepareKey(key)
    val rc = cursor.seekPosition(keyDirectBuffer, valDirectBuffer, SeekOp.KEY)
    if (rc != 0) Array.empty else bytesFromDirectBuffer(valDirectBuffer)
  }
  def set(key: RawKey, value: RawValue): Unit = {
    prepareKey(key)
    if(value.length==0){
      val rc = cursor.seekPosition(keyDirectBuffer, valDirectBuffer, SeekOp.KEY)
      if (rc != 0){ return }
      cursor.delete()
    }else{
      bytesToDirectBuffer(valByteBuffer, valDirectBuffer, value)
      cursor.put(keyDirectBuffer, valDirectBuffer, 0)
    }
  }
  private def keyStatus(rc: Int) = peek = if (rc != 0) NotFoundStatus else new KeyStatus(
    bytesFromDirectBuffer(keyDirectBuffer),
    bytesFromDirectBuffer(valDirectBuffer)
  )
  def seek(from: RawKey): Unit = {
    prepareKey(from)
    keyStatus(cursor.seekPosition(keyDirectBuffer, valDirectBuffer, SeekOp.RANGE))
  }
  def seekNext() = {
    if(peek == NotFoundStatus) Never()
    keyStatus(cursor.position(keyDirectBuffer, valDirectBuffer, GetOp.NEXT))
  }
}
