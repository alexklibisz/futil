package futil

import java.util.concurrent.{Callable, ScheduledExecutorService, ScheduledThreadPoolExecutor}
import scala.concurrent._
import scala.concurrent.duration._
import scala.util.Try

/** Utilities to get more from Scala Futures. */
object Futil {

  object Implicits {
    lazy implicit val scheduler: ScheduledExecutorService = new ScheduledThreadPoolExecutor(1)
  }

  /**
    * Lift a call-by-name Future[A] into a () => Future[A].
    */
  final def thunk[A](fa: => Future[A]): () => Future[A] = () => fa

  /**
    * Times the execution of the Future with nanosecond precision.
    */
  final def timed[A](fa: => Future[A])(implicit ec: ExecutionContext): Future[(A, Duration)] = {
    val t0 = System.nanoTime()
    fa.map(_ -> (System.nanoTime() - t0).nanos)
  }

  /**
    * If the Future takes more than the given Duration to complete, return a failed Future with a TimeoutException.
    */
  final def deadline[A](
      duration: Duration
  )(fa: => Future[A])(implicit ec: ExecutionContext, scheduler: ScheduledExecutorService): Future[A] = {
    val ex = new TimeoutException(s"The given future did not complete within the given duration: $duration.")
    val other = delay(duration)(Future.failed(ex))
    Future.firstCompletedOf(Seq(fa, other))
  }

  /**
    * Run the Future after delaying for the given Duration.
    */
  final def delay[A](
      duration: Duration
  )(fa: => Future[A])(implicit ec: ExecutionContext, scheduler: ScheduledExecutorService): Future[A] = {
    val p = Promise[A]()
    val t = new Callable[Unit] {
      override def call(): Unit = fa.onComplete(p.complete)
    }
    scheduler.schedule(t, duration._1, duration._2)
    p.future
  }

  /**
    * Asynchronously sleep for the given duration.
    */
  final def sleep(
      duration: Duration
  )(implicit ec: ExecutionContext, scheduler: ScheduledExecutorService): Future[Unit] =
    delay(duration)(Future.successful(()))

  /**
    * Use function f to map each element in as to a Future[B], running at most n Futures at a time.
    * Lifts the result of the Future[B] into a Future[Try[B]\] to prevent failing the entire Seq.
    * Results are returned in the original order.
    */
  final def traverseParN[A, B](
      n: Int
  )(as: Seq[A])(f: A => Future[B])(implicit ec: ExecutionContext): Future[Seq[Try[B]]] = {
    val sem = semaphore(n)
    Future.sequence(as.map(a => sem.withPermit(thunk(f(a).transformWith(Future.successful)))))
  }

  /**
    * Alias for [[traverseParN]] with n = 1.
    */
  final def traverseSerial[A, B](as: Seq[A])(f: A => Future[B])(implicit ec: ExecutionContext): Future[Seq[Try[B]]] =
    traverseParN(1)(as)(f)

  /**
    * Retry the given Future according to the given [[RetryPolicy]].
    */
  final def retry[A](
      policy: RetryPolicy[A]
  )(fa: () => Future[A])(implicit ec: ExecutionContext, scheduler: ScheduledExecutorService): Future[A] =
    fa().transformWith { t =>
      policy match {
        case p @ RetryPolicy.Repeat(n, d) =>
          if (n > 0) d(t).flatMap(if (_) Future.fromTry(t) else retry(p.copy(n - 1))(fa))
          else Future.fromTry(t)
        case p @ RetryPolicy.FixedBackoff(n, w, d) =>
          if (n > 0) d(t).flatMap(if (_) Future.fromTry(t) else delay(w)(retry(p.copy(n - 1))(fa)))
          else Future.fromTry(t)
        case p @ RetryPolicy.ExponentialBackoff(n, w, d) =>
          if (n > 0) d(t).flatMap(if (_) Future.fromTry(t) else delay(w)(retry(p.copy(n - 1, w * 2))(fa)))
          else Future.fromTry(t)
      }
    }

  /**
    * Construct an [[AsyncSemaphore]] with the specified number of permits.
    */
  final def semaphore(permits: Int): AsyncSemaphore = {
    require(permits > 0, "semaphore must have at least 1 permit")
    new AsyncSemaphore(permits)
  }

}
