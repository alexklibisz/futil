package futil

import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers

import java.util.concurrent.atomic.AtomicInteger
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import scala.util._

class ParallelismSpec extends AsyncFreeSpec with Matchers {

  override implicit val executionContext: ExecutionContext =
    ExecutionContext.global

  import Futil.Implicits.timer

  "mapParN" - {

    "all successful" in {
      val as = (1 to 1000).toVector
      val f = (i: Int) => Future.successful(i)
      Futil.mapParN(10)(as)(f).flatMap { bs =>
        bs shouldBe as.map(Success(_))
      }
    }

    "all failed" in {
      final case class Expected(i: Int) extends Throwable
      val as = (1 to 1000).toVector
      val f = (i: Int) => Future.failed[Int](Expected(i))
      Futil.mapParN(10)(as)(f).flatMap { bs =>
        bs shouldBe as.map(Expected).map(Failure(_))
      }
    }

    "some successful some failed" in {
      final case class Expected(i: Int) extends Throwable
      val as = (1 to 1000).toVector
      val f = (i: Int) => if (i % 2 == 0) Future.successful(i) else Future.failed(Expected(i))
      Futil.mapParN(10)(as)(f).flatMap { bs =>
        bs shouldBe as.map(i => if (i % 2 == 0) Success(i) else Failure(Expected(i)))
      }
    }

    "execute no more than n at a time" in {
      val counter = new AtomicInteger(0)
      val as = (1 to 1000).toVector
      val n = 10
      val f = (i: Int) =>
        Future {
          val c = counter.getAndIncrement()
          if (c > n) fail(s"$c > $n")
          Thread.sleep(i % 30)
          counter.decrementAndGet()
          i
        }

      Futil.mapParN(n)(as)(f).flatMap { bs =>
        bs shouldBe as.map(Success(_))
      }
    }

    "delays are pipelined" in {

      // The first 9 tasks each runs for 1000ms the remaining 100 run for 10ms each. 1 second total.
      // ((9 * 1000ms) + (100 * 10ms)) / 10 = 1000ms
      val as = (1 to 9).map(_ => 1000) ++ (1 to 100).map(_ => 10)
      val n = 10
      val f = (a: Int) => Futil.delay(a.millis)(Future.successful(()))

      Futil.timed(Futil.mapParN(n)(as)(f)).map {
        case (_, dur) => dur.toMillis shouldBe 1000L +- 50
      }
    }

    "fibonacci numbers are parallelized" in {

      // Check that computing fibonacci numbers is faster using mapParN with n = 4 than n = 1.

      def fib(n: Int): Int = if (n <= 1) n else fib(n - 1) + fib(n - 2)

      val as = (1 to 42).toVector
      val f = (i: Int) => Future(fib(i))

      for {
        (res1, dur1) <- Futil.timed(Futil.mapParN(1)(as)(f))
        (res4, dur4) <- Futil.timed(Futil.mapParN(4)(as)(f))
      } yield {
        res1.toVector shouldBe res4.toVector
        dur1.toMillis shouldBe >(dur4.toMillis)
      }
    }

  }

}
