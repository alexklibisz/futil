package futil

import java.util.concurrent.atomic.AtomicReference
import scala.concurrent.{ExecutionContext, Future, Promise}

/**
  * Semaphore that asynchronously grants a fixed number of permits.
  */
final class AsyncSemaphore private (permits: Int) extends Serializable {

  private case class State(available: Int, waiting: Vector[Promise[Unit]], dummy: Future[Unit])

  private val funit = Future.successful(())
  private val state = new AtomicReference[State](State(permits, Vector.empty[Promise[Unit]], funit))

  def acquire(): Future[Unit] = {
    val s_ = state.updateAndGet { s =>
      if (s.available > 0) s.copy(s.available - 1, dummy = funit)
      else {
        val p = Promise[Unit]()
        s.copy(waiting = s.waiting :+ p, dummy = p.future)
      }
    }
    s_.dummy
  }

  def release(): Future[Unit] = {
    val s_ = state.updateAndGet { s =>
      val available = if (s.available < permits) s.available + 1 else permits
      if (s.waiting.isEmpty) s.copy(available, dummy = funit)
      else {
        s.waiting.head.trySuccess(()) // Because the update can run multiple times.
        State(available, s.waiting.tail, funit)
      }
    }
    s_.dummy
  }

  def withPermit[A](f: () => Future[A])(implicit ec: ExecutionContext): Future[A] =
    for {
      _ <- acquire()
      ta <- f().transformWith(Future.successful) // Lift to Future[Try[A]] to avoid short-circuiting on failure.
      _ <- release()
      a <- Future.fromTry(ta) // Drop back to a Future[A].
    } yield a

  private[futil] def inspect(): Future[(Int, Int)] = {
    val s = state.get()
    Future.successful((s.available, s.waiting.length))
  }

}

object AsyncSemaphore {
  def apply(permits: Int): AsyncSemaphore = {
    require(permits > 0, "AsyncSemaphore must have at least 1 permit")
    new AsyncSemaphore(permits)
  }
}
