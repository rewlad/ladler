package io.github.rewlad

import java.util
import java.util.concurrent.Executor

import io.github.rewlad.sseserver.ToRunnable

class SerialExecutor(executor: Executor) {
  private lazy val tasks = new util.ArrayDeque[Runnable]
  private var active: Option[Runnable] = None
  def apply(f: ⇒Unit) = synchronized {
    tasks.offer(ToRunnable{ try f finally scheduleNext() })
    if(active.isEmpty) scheduleNext()
  }
  private def scheduleNext(): Unit = synchronized {
    active = Option(tasks.poll())
    active.foreach(executor.execute)
  }
}
