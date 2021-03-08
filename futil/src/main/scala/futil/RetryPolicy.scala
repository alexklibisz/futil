package futil

import scala.concurrent.duration.Duration

sealed trait RetryPolicy

object RetryPolicy {

  /**
    * Retry up to n times or stop if earlyStop returns true.
    */
  final case class Repeat(n: Int) extends RetryPolicy

  /**
    * Retry up to n times, delaying by duration between each try. Stop if earlyStop returns true.
    */
  final case class FixedBackoff(n: Int, duration: Duration) extends RetryPolicy

  /**
    * Retry up to n times, doubling the initial duration between each try. Stop if earlyStop returns true.
    */
  final case class ExponentialBackoff(n: Int, duration: Duration) extends RetryPolicy

}
