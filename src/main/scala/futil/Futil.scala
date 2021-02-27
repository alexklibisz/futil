package futil

import java.util.concurrent.atomic.{AtomicInteger, AtomicReferenceArray}
import java.util.{Timer, TimerTask}
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future, Promise, TimeoutException}
import scala.util.Try

trait Futil {

  /**
    * Time the execution of the given Future with nanosecond precision.
    * The duration is only returned if the Future succeeds.
    */
  final def timed[A](fa: => Future[A])(implicit ec: ExecutionContext): Future[(A, Duration)] = {
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
    * Map over the elements of the given `IndexedSeq[A]` and apply the function `f` to start a `Future[B]`.
    * Ensures that no more than `n` `Future[B]` are executing at any given time.
    * Lifts the result of the `Future[B]` into a `Future[Try[B]]` to prevent failing the `Future[Iterable[Try[B]]`.
    * Results are returned in the original order.
    */
  final def mapParN[A, B](n: Int)(as: IndexedSeq[A])(f: A => Future[B])(implicit ec: ExecutionContext): Future[IndexedSeq[Try[B]]] =
    if (as.length <= n) Future.sequence(as.map(f(_).transformWith(Future.successful)))
    else {
      // Completion is signaled by completing a dummy promise.
      val finished = Promise[Unit]()

      // Bookkeeping maintained using java atomic utils.
      val results = new AtomicReferenceArray[Try[B]](as.length)
      val completed = new AtomicInteger(0)
      val nextIndex = new AtomicInteger(n)

      // Define the behavior for completing one future and starting the next.
      def startNext(tb: Try[B], ixCompleted: Int): Unit = {
        results.set(ixCompleted, tb)
        if (completed.incrementAndGet() == as.length) finished.success(())
        else {
          val ixCurr = nextIndex.getAndIncrement()
          if (ixCurr < as.length) f(as(ixCurr)).transformWith(Future.successful).foreach(startNext(_, ixCurr))
        }
      }

      // Start the first `n` futures.
      as.take(n).zipWithIndex.foreach {
        case (a, i) => f(a).transformWith(Future.successful).foreach(startNext(_, i))
      }

      // Upon completion, grab all of the A's out of the atomic reference array and into an indexedseq.
      finished.future.map(_ => as.indices.map(results.get))
    }

  /**
    * Alias for [[mapParN]] with n = 1.
    */
  final def mapSerial[A, B](as: IndexedSeq[A])(f: A => Future[B])(implicit ec: ExecutionContext): Future[IndexedSeq[Try[B]]] =
    mapParN(1)(as)(f)

  /**
    * Retry the given Future according to the given [[RetryPolicy]].
    */
  final def retry[A](policy: RetryPolicy[A])(fa: () => Future[A])(implicit ec: ExecutionContext, timer: Timer): Future[A] =
    fa().transformWith { t =>
      policy match {
        case p @ RetryPolicy.Repeat(n, d) =>
          if (n > 0) d(t).flatMap(b => if (b) Future.fromTry(t) else retry(p.copy(n - 1))(fa))
          else Future.fromTry(t)
        case p @ RetryPolicy.FixedBackoff(n, w, d) =>
          if (n > 0) d(t).flatMap(b => if (b) Future.fromTry(t) else delay(w)(retry(p.copy(n - 1))(fa)))
          else Future.fromTry(t)
        case p @ RetryPolicy.ExponentialBackoff(n, w, d) =>
          if (n > 0) d(t).flatMap(b => if (b) Future.fromTry(t) else delay(w)(retry(p.copy(n - 1, w * 2))(fa)))
          else Future.fromTry(t)
      }
    }

}

object Futil extends Futil {
  object Implicits {
    lazy implicit val timer: Timer = new Timer("futil-timer", true)
  }
}
