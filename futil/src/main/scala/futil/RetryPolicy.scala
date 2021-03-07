package futil

import scala.concurrent.Future
import scala.concurrent.duration.Duration
import scala.util.Try

sealed trait RetryPolicy[T]

object RetryPolicy {

  private def earlyStopOnSuccess[T]: Try[T] => Future[Boolean] = (t: Try[T]) => Future.successful(t.isSuccess)

  /**
    * Retry up to n times or stop if earlyStop returns true.
    */
  final case class Repeat[T](n: Int, earlyStop: Try[T] => Future[Boolean]) extends RetryPolicy[T]

  object Repeat {
    def apply[T](n: Int): Repeat[T] = Repeat(n, earlyStopOnSuccess)
  }

  /**
    * Retry up to n times, delaying by duration between each try. Stop if earlyStop returns true.
    */
  final case class FixedBackoff[T](n: Int, duration: Duration, earlyStop: Try[T] => Future[Boolean])
      extends RetryPolicy[T]

  object FixedBackoff {
    def apply[T](n: Int, duration: Duration): FixedBackoff[T] = FixedBackoff(n, duration, earlyStopOnSuccess)
  }

  /**
    * Retry up to n times, doubling the initial duration between each try. Stop if earlyStop returns true.
    */
  final case class ExponentialBackoff[T](n: Int, duration: Duration, earlyStop: Try[T] => Future[Boolean])
      extends RetryPolicy[T]

  object ExponentialBackoff {
    def apply[T](n: Int, duration: Duration): ExponentialBackoff[T] =
      ExponentialBackoff(n, duration, earlyStopOnSuccess)
  }

}
