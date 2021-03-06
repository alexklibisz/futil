package futil

import java.util.concurrent.atomic.AtomicReference
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.control.NonFatal

/**
  * Semaphore that asynchronously grants a fixed number of permits.
  */
private[futil] final class AsyncSemaphore private (permits: Int) extends Serializable {

  // Have to keep a Future[Unit] in the state to work with atomic updateAndGet.
  private case class State(available: Int, waiting: Vector[Promise[Unit]], last: Future[Unit])

  private val funit = Future.successful(())
  private val state = new AtomicReference[State](State(permits, Vector.empty[Promise[Unit]], funit))

  /**
    * Returns a Future which will complete when a permit becomes available.
    */
  def acquire(): Future[Unit] = {
    val s_ = state.updateAndGet { s =>
      if (s.available > 0) s.copy(s.available - 1, last = funit)
      else {
        val p = Promise[Unit]()
        s.copy(waiting = s.waiting :+ p, last = p.future)
      }
    }
    s_.last
  }

  /**
    * Returns a Future which completes immediately and releases another waiting future
    */
  def release(): Future[Unit] = {
    val s_ = state.updateAndGet { s =>
      if (s.waiting.isEmpty) s.copy(if (s.available < permits) s.available + 1 else permits, last = funit)
      else // Need try because the update method can potentially be called multiple times.
        s.copy(waiting = s.waiting.tail, last = Future.successful(s.waiting.head.trySuccess(())))
    }
    s_.last
  }

  /**
    * Execute the given Future _after_ acquiring a permit and then release the permit, even if the Future failed.
    */
  def withPermit[A](f: () => Future[A])(implicit ec: ExecutionContext): Future[A] =
    for {
      _ <- acquire()
      a <- f().recoverWith {
        case NonFatal(e) => release().flatMap(_ => Future.failed(e))
      }
      _ <- release()
    } yield a

  private[futil] def inspect(): Future[(Int, Int)] = {
    val s = state.get()
    Future.successful((s.available, s.waiting.length))
  }

}

private[futil] object AsyncSemaphore {
  def apply(permits: Int): AsyncSemaphore = {
    require(permits > 0, "AsyncSemaphore must have at least 1 permit")
    new AsyncSemaphore(permits)
  }
}
