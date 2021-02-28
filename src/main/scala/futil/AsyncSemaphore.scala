package futil

import java.util
import scala.concurrent.{ExecutionContext, Future, Promise}

/**
  * Semaphore that asynchronously grants a fixed number of permits.
  */
final class AsyncSemaphore private (permits: Int) extends Serializable {

  private var available = permits
  private val waiting = new util.ArrayDeque[Promise[Unit]]

  def acquire(): Future[Unit] = waiting.synchronized {
    if (available > 0) Future.successful(available -= 1)
    else {
      val p = Promise[Unit]()
      waiting.addLast(p)
      p.future
    }
  }

  def release(): Future[Unit] = waiting.synchronized {
    if (waiting.isEmpty) Future.successful(if (available < permits) available += 1 else ())
    else waiting.removeFirst().success(()).future
  }

  def withPermit[A](f: () => Future[A])(implicit ec: ExecutionContext): Future[A] =
    for {
      _ <- acquire()
      ta <- f().transformWith(Future.successful) // Lift to a Future[Try[A]] to avoid short-circuiting on failure.
      _ <- release()
      a <- Future.fromTry(ta) // Drop back to a Future[A].
    } yield a

}

object AsyncSemaphore {
  def apply(permits: Int): AsyncSemaphore = {
    require(permits > 0, "AsyncSemaphore must have at least 1 permit")
    new AsyncSemaphore(permits)
  }
}
