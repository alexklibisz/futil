package futil

import java.util.concurrent.{Callable, Executors, ScheduledExecutorService, ThreadFactory}
import scala.collection.compat._
import scala.concurrent.Future.{fromTry, successful}
import scala.concurrent._
import scala.concurrent.duration._
import scala.util._

/** Utilities to get more from Scala Futures. */
object Futil {

  object Implicits {
    lazy implicit val scheduler: ScheduledExecutorService = {
      // Need a daemon thread factory, otherwise an app will hang if the scheduler is not explicitly terminated.
      val tf = new ThreadFactory {
        override def newThread(runnable: Runnable): Thread = {
          val t = Executors.defaultThreadFactory().newThread(runnable)
          t.setDaemon(true)
          t.setName("futil-default-scheduler")
          t
        }
      }
      Executors.newSingleThreadScheduledExecutor(tf)
    }
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
    * Use function f to map each element from `in` to a Future[B], running at most n Futures at a time.
    * Lifts the result of the Future[B] into a Future[Try[B]\] to prevent failing the entire Seq.
    * Results are returned in the original order.
    */
  final def traverseParN[A, B, M[X] <: IterableOnce[X]](
      n: Int
  )(in: M[A])(fn: A => Future[B])(implicit bf: BuildFrom[M[A], Try[B], M[Try[B]]],
                                  ec: ExecutionContext): Future[M[Try[B]]] = {
    val sem = semaphore(n)
    in.iterator
      .foldLeft(successful(bf.newBuilder(in))) { (f1, a: A) =>
        val f2 = sem.withPermit(thunk(fn(a).transformWith(Future.successful)))
        f1.zipWith(f2)(_ += _)
      }
      .map(_.result())(ec)
  }

  /**
    * Alias for [[traverseParN]] with n = 1.
    */
  final def traverseSerial[A, B, M[X] <: IterableOnce[X]](in: M[A])(
      fn: A => Future[B])(implicit bf: BuildFrom[M[A], Try[B], M[Try[B]]], ec: ExecutionContext): Future[M[Try[B]]] =
    traverseParN(1)(in)(fn)

  /**
    * Retry the given Future according to the given [[RetryPolicy]].
    */
  final def retry[A](
      policy: RetryPolicy,
      earlyStop: Try[A] => Future[Boolean] = (t: Try[A]) => Future.successful(t.isSuccess)
  )(fa: () => Future[A])(implicit ec: ExecutionContext, scheduler: ScheduledExecutorService): Future[A] =
    fa().transformWith { t =>
      policy match {
        case p @ RetryPolicy.Repeat(n) =>
          if (n > 0) earlyStop(t).flatMap(if (_) fromTry(t) else retry(p.copy(n - 1), earlyStop)(fa))
          else fromTry(t)
        case p @ RetryPolicy.FixedBackoff(n, w) =>
          if (n > 0) earlyStop(t).flatMap(if (_) fromTry(t) else delay(w)(retry(p.copy(n - 1), earlyStop)(fa)))
          else fromTry(t)
        case p @ RetryPolicy.ExponentialBackoff(n, w) =>
          if (n > 0) earlyStop(t).flatMap(if (_) fromTry(t) else delay(w)(retry(p.copy(n - 1, w * 2), earlyStop)(fa)))
          else fromTry(t)
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
