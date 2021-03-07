package futil

import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers

import java.util.concurrent.atomic.AtomicInteger
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util._

class ParallelismSpec extends AsyncFreeSpec with GlobalExecutionContext with Matchers {

  import Futil.Implicits.timer

  "traverseParN" - {

    "all successful" in {
      val as = (1 to 1000).toVector
      val f = (i: Int) => Future.successful(i)
      Futil.traverseParN(10)(as)(f).flatMap { bs =>
        bs shouldBe as.map(Success(_))
      }
    }

    "all failed" in {
      final case class Expected(i: Int) extends Throwable
      val as = (1 to 1000).toVector
      val f = (i: Int) => Future.failed[Int](Expected(i))
      Futil.traverseParN(10)(as)(f).flatMap { bs =>
        bs shouldBe as.map(Expected).map(Failure(_))
      }
    }

    "some successful some failed" in {
      final case class Expected(i: Int) extends Throwable
      val as = (1 to 1000).toVector
      val f = (i: Int) => if (i % 2 == 0) Future.successful(i) else Future.failed(Expected(i))
      Futil.traverseParN(10)(as)(f).flatMap { bs =>
        bs shouldBe as.map(i => if (i % 2 == 0) Success(i) else Failure(Expected(i)))
      }
    }

    "execute no more than n at a time" in {
      val counter = new AtomicInteger(0)
      val as = (1 to 10000).toVector
      val n = 10
      val f = (i: Int) =>
        for {
          _ <- Future {
            val c = counter.getAndIncrement()
            if (c > n) fail(s"$c > $n")
          }
          _ <- Futil.delay(1.millis)(Future.successful(()))
          _ <- Future {
            counter.decrementAndGet()
          }
        } yield i

      Futil.traverseParN(n)(as)(f).flatMap { bs =>
        bs.filter(_.isFailure) shouldBe Seq.empty
      }
    }

    "delays are pipelined" in {

      // The first 9 tasks each runs for 1000ms the remaining 100 run for 10ms each. 1 second total.
      // ((9 * 1000ms) + (100 * 10ms)) / 10 = 1000ms
      val as = (1 to 9).map(_ => 1000) ++ (1 to 100).map(_ => 10)
      val n = 10
      val f = (a: Int) => Futil.delay(a.millis)(Future.successful(()))

      Futil.timed(Futil.traverseParN(n)(as)(f)).map {
        case (_, dur) => dur.toMillis shouldBe 1000L +- 50
      }
    }

    def fib(n: Int): Int = if (n <= 1) n else fib(n - 1) + fib(n - 2)

    "fibonacci is faster with n = 2 than n = 1" in {
      // Check that computing fibonacci numbers is faster using traverseParN() with n = 2 than n = 1.
      val as = (1 to 500).map(_ % 42)
      val f = (i: Int) => Future(fib(i))
      for {
        (res2, dur2) <- Futil.timed(Futil.traverseParN(2)(as)(f))
        (res1, dur1) <- Futil.timed(Futil.traverseParN(1)(as)(f))
      } yield {
        res1.toVector shouldBe res2.toVector
        dur1.toMillis shouldBe >(dur2.toMillis)
      }
    }

    "1 million fibonaccis (many small cpu-bound tasks)" in {
      def fib(n: Int): Int = if (n <= 1) n else fib(n - 1) + fib(n - 2)
      val as = (0 to 999999).map(_ % 28)
      val f = (i: Int) => Future(fib(i))
      Futil.traverseParN(2)(as)(f).flatMap(_.toVector.length shouldBe as.length)
    }

    "1 million delays (many small async tasks)" in {
      val as = (0 to 999999).map(_ % 30000 + 10000)
      val f = (t: Int) => Futil.delay(t.nanos)(Future.successful(()))
      Futil.traverseParN(2)(as)(f).flatMap(_.toVector.length shouldBe as.length)
    }

    "1 million mixed delays and fibonaccis" in {
      val as = (0 to 999999)
      val f = (i: Int) =>
        if (i % 2 == 0) Future(fib(i % 28)) else Futil.delay((i % 30000 + 10000).nanos)(Future.successful(()))
      Futil.traverseParN(2)(as)(f).flatMap(_.toVector.length shouldBe as.length)
    }

  }

  "traverseSerial" - {

    "executes serially" in {
      val counter = new AtomicInteger(0)
      val as = (1 to 10000).toVector
      val f = (i: Int) =>
        for {
          _ <- Future {
            val c = counter.getAndIncrement()
            if (c > 1) fail(s"$c > 1")
          }
          _ <- Futil.delay(1.millis)(Future.successful(()))
          _ <- Future {
            counter.decrementAndGet()
          }
        } yield i

      Futil.traverseSerial(as)(f).flatMap { bs =>
        bs.filter(_.isFailure) shouldBe Seq.empty
      }
    }

  }

}
