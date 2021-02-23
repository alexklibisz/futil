package futil

import futil.FUtil.time

import java.util.Timer
import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

trait FUtil {

  /**
   * Run both Futures and return the result of the first one to complete.
   * Will not cancel the incomplete Future.
   */
  final def race[A](fa1: => Future[A], fa2: => Future[A])(implicit ec: ExecutionContext, timer: Timer): Future[A] = ???

  /**
   * Run the given Future after the given Duration.
   * If the Future is already running, then this will simply delay the return of the Future.
   */
  final def delay[A](duration: Duration)(fa: => Future[A])(implicit ec: ExecutionContext, timer: Timer): Future[A] = ???

  /**
   * Run the given Future for at most the given Duration.
   * If it takes longer than the given direction, return a Future.failed containing a TimeoutException.
   */
  final def deadline[A](duration: Duration)(fa: => Future[A])(implicit ec: ExecutionContext, timer: Timer): Future[A] = ???

  /**
   * Retry the given Future according to the given Schedule.
   */
  final def retry[A](schedule: Schedule)(fa: => Future[A])(implicit ec: ExecutionContext, timer: Timer): Future[A] = ???

  /**
   * Retry the given Future at most N times.
   */
  final def retryN[A](n: Int)(fa: => Future[A])(implicit ec: ExecutionContext): Future[A] = ???

  // TODO: a way to predicate the retry based on the failure.

  /**
   * Map each A in the given Iterable[A] to a Future[B], running at most n Future[B] at a time.
   * The given Iterable[A] is traversed in order, but the resulting Iterable[B] is unordered.
   * If one of the Futures fails, return the Throwable in a failed Future and don't start any new Futures.
   */
  final def mapUnorderedParN[A, B](n: Int)(in: => Iterable[A])(f: A => Future[B])(implicit ec: ExecutionContext): Future[Iterable[B]] = ???

  /**
   * Map each A in the given Iterable[A] to a Future[Try[B]\], running at most n Future[Try[B]\] at a time.
   * The given Iterable[A] is traversed in order, but the resulting Iterable[B] is unordered.
   *
   * If continueOnFailure is true, continue starting new Futures even if some of them have failed.
   * The failed Futures will be represented as a failed Try.
   *
   * If continueONFailure is false, don't start any more new Futures when the first Future has failed.
   * Complete any running Futures and return all of the completed Try[B]'s.
   */
  final def mapUnorderedTryParN[A, B](n: Int, continueOnFailure: Boolean = true)(in: => Iterable[A])(f: A => Future[B])(implicit ec: ExecutionContext): Future[Iterable[Try[B]]] = ???

  /**
   * Map each A in the given Iterable[A] to a Future[Unit], running at most n Future[Unit] at a time.
   * If one of the Futures fails, return the failure in a failed Future and don't start any new Futures.
   */
  final def foreachTryParN[A](n: Int)(in: Iterable[A])(f: A => Future[Unit])(implicit ec: ExecutionContext): Future[Unit] = ???

  /**
   * Time the execution of the given Future with nanosecond precision.
   * The duration is only returned if the Future succeeds.
   */
  final def time[A](fa: => Future[A])(implicit ec: ExecutionContext): Future[(A, Duration)] = ???

  /**
   * Time the execution of the given Future with nanosecond precision.
   * If the Future succeeds, the duration is returned alongside a successful Try[A].
   * If the Future fails, the duration is returned alongside a failed Try[A].
   */
  final def timeTry[A](fa: => Future[A])(implicit ec: ExecutionContext): Future[(Try[A], Duration)] = ???

}

object FUtil extends FUtil {
  implicit val timer: Timer = new Timer("FUtil timer", true)
}

object A extends App {
  implicit val ec: ExecutionContext = ExecutionContext.global
  val a = Future.successful(1)
  val b = time(a)
}
