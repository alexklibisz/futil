package futil

import java.util.{Timer, TimerTask}
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future, Promise, TimeoutException}

trait Futil {

  /**
   * Time the execution of the given Future with nanosecond precision.
   * The duration is only returned if the Future succeeds.
   */
  final def time[A](fa: => Future[A])(implicit ec: ExecutionContext): Future[(A, Duration)] = {
    val t0 = System.nanoTime()
    fa.map(_ -> (System.nanoTime() - t0).nanos)
  }

  /**
   * Run the given Future after the given Duration.
   * If the Future is already running, then this will simply delay the return of the Future.
   */
  final def delay[A](duration: Duration)(fa: => Future[A])(implicit ec: ExecutionContext, timer: Timer): Future[A] = {
    val p = Promise[A]()
    val t = new TimerTask {
      override def run(): Unit = fa.onComplete(p.complete)
    }
    timer.schedule(t, duration.toMillis)
    p.future
  }

  /**
   * Run the given Future for at most the given Duration.
   * If it takes longer than the given direction, return a Future.failed containing a TimeoutException.
   */
  final def deadline[A](duration: Duration)(fa: => Future[A])(implicit ec: ExecutionContext, timer: Timer): Future[A] = {
    val deadline = delay(duration)(Future.failed(new TimeoutException))
    Future.firstCompletedOf(Seq(fa, deadline))
  }

  /**
   * Retry the given Future according to the given Schedule.
   */
  final def retry[A](policy: RetryPolicy)(fa: => Future[A])(implicit ec: ExecutionContext, timer: Timer): Future[A] = ???

  /**
   * Applies given function `fn` to each element of the `Iterable[A]` as in parallel, running at most `n` in parallel.
   */
  final def foreachParN[A, B](n: Int)(as: Iterable[A])(fn: A => Future[B])(implicit ec: ExecutionContext): Future[Iterable[B]] = ???

}

object Futil extends Futil {
  object Implicits {
    lazy implicit val timer: Timer = new Timer("futil-timer", true)
  }
}