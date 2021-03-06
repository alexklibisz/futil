package futil

import org.scalatest.Inspectors
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}

class RetrySpec extends AsyncFreeSpec with GlobalExecutionContext with Matchers with Inspectors {

  case class Expected() extends Throwable

  import Futil.Implicits.scheduler
  import RetryPolicy._

  "repeat" - {

    "all failures" in {
      val failures = ArrayBuffer.empty[Expected]
      val f = () =>
        Future {
          failures.append(Expected())
          throw Expected()
        }
      Futil.retry(Repeat(3))(f).transformWith { t =>
        t shouldBe Failure(Expected())
        failures.toList should have length 4
      }
    }

    "eventually succeeds" in {
      val failures = ArrayBuffer.empty[Expected]
      val f = () =>
        Future {
          if (failures.length > 2) ()
          else {
            failures.append(Expected())
            throw Expected()
          }
        }
      Futil.retry(Repeat(3))(f).transformWith { t =>
        t shouldBe Success(())
        failures.length shouldBe 3
      }
    }

    "thousands of retries" in {
      var counter = 0
      val f = () => Future.failed { counter += 1; Expected() }
      Futil.retry(Repeat(9999))(f).transformWith { t =>
        t shouldBe Failure(Expected())
        counter shouldBe 10000
      }
    }

    "early stop" in {
      var counter = 0
      val f = () => Future.failed { counter += 1; Expected() }
      val decision = (_: Try[Unit]) => Future.successful(counter >= 10)
      val policy = Repeat(99)
      Futil.retry(policy, decision)(f).transformWith { t =>
        t shouldBe Failure(Expected())
        counter shouldBe 10
      }
    }

  }

  "fixed backoff" - {

    "all failures" in {
      val failures = ArrayBuffer.empty[Expected]
      val f = () =>
        Future {
          failures.append(Expected())
          throw Expected()
        }
      val r = Futil.retry(FixedBackoff(3, 1000.millis))(f)
      Futil.timed(r.transformWith(Future.successful)).flatMap {
        case (t, duration: Duration) =>
          t shouldBe Failure(Expected())
          info(duration.toString)
          duration.toSeconds shouldBe 3
      }
    }

    "eventually succeeds" in {
      val failures = ArrayBuffer.empty[Expected]
      val f = () =>
        Future {
          if (failures.length > 4) ()
          else {
            failures.append(Expected())
            throw Expected()
          }
        }
      val r = Futil.retry(FixedBackoff(10, 200.millis))(f)
      Futil.timed(r.transformWith(Future.successful)).flatMap {
        case (t, duration: Duration) =>
          t shouldBe Success(())
          info(duration.toString)
          duration.toSeconds shouldBe 1
          failures.length shouldBe 5
      }
    }

    "times are evenly spaced" in {
      val times = ArrayBuffer.empty[Long]
      val f = () =>
        Future {
          times.append(System.nanoTime())
          throw Expected()
        }
      Futil.retry(FixedBackoff(10, 100.millis))(f).transformWith { t =>
        t shouldBe Failure(Expected())
        val gaps = times.zip(times.tail).map(t => t._2 - t._1).map(_.nanos.toMillis)
        forAll(gaps)(_ shouldBe 100L +- 10L)
      }
    }

    "early stop" in {
      var counter = 0
      val f = () => Future.failed { counter += 1; Expected() }
      val decision = (_: Try[Unit]) => Future.successful(counter >= 10)
      val policy = FixedBackoff(99, 100.millis)
      Futil.retry(policy, decision)(f).transformWith { t =>
        t shouldBe Failure(Expected())
        counter shouldBe 10
      }
    }

  }

  "exponential backoff" - {

    "all failures" in {
      val failures = ArrayBuffer.empty[Expected]
      val f = () =>
        Future {
          failures.append(Expected())
          throw Expected()
        }
      val r = Futil.retry(ExponentialBackoff(3, 100.millis))(f)
      Futil.timed(r.transformWith(Future.successful)).flatMap {
        case (t, duration: Duration) =>
          t shouldBe Failure(Expected())
          duration.toMillis shouldBe (100L + 200L + 400L) +- (50L)
      }
    }

    "eventually succeeds" in {
      val failures = ArrayBuffer.empty[Expected]
      val f = () =>
        Future {
          if (failures.length > 4) ()
          else {
            failures.append(Expected())
            throw Expected()
          }
        }
      val r = Futil.retry(ExponentialBackoff(10, 100.millis))(f)
      Futil.timed(r.transformWith(Future.successful)).flatMap {
        case (t, duration: Duration) =>
          t shouldBe Success(())
          duration.toMillis shouldBe (100L + 200L + 400L + 800L + 1600L) +- 50L
          failures.length shouldBe 5
      }
    }

    "times are doubled" in {
      val times = ArrayBuffer.empty[Long]
      val f = () =>
        Future {
          times.append(System.nanoTime())
          throw Expected()
        }
      Futil.retry(ExponentialBackoff(5, 100.millis))(f).transformWith { t =>
        t shouldBe Failure(Expected())
        val gaps = times.zip(times.tail).map(t => t._2 - t._1).map(_.nanos.toMillis / 100d).toVector
        val expected = Vector(1d, 2d, 4d, 8d, 16d)
        forAll(gaps.zip(expected)) {
          case (gap, exp) => gap shouldBe exp +- (0.1d)
        }
      }
    }

    "early stop" in {
      var counter = 0
      val f = () => Future.failed { counter += 1; Expected() }
      val decision = (_: Try[Unit]) => Future.successful(counter >= 10)
      val policy = ExponentialBackoff(99, 2.millis)
      Futil.retry(policy, decision)(f).transformWith { t =>
        t shouldBe Failure(Expected())
        counter shouldBe 10
      }
    }

  }

}
