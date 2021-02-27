package futil

import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers

import scala.concurrent.duration.DurationLong
import scala.concurrent.{ExecutionContext, Future, TimeoutException}

class TimingSpec extends AsyncFreeSpec with Matchers {

  override implicit val executionContext: ExecutionContext = ExecutionContext.global

  import Futil.Implicits.timer

  "time" - {
    "100 millis" in {
      lazy val fa: Future[Int] = Future { Thread.sleep(100); 99 }
      val t0 = System.nanoTime()
      Futil.timed(fa).flatMap {
        case (i, duration) =>
          val dur = (System.nanoTime() - t0).nanos
          i shouldBe 99
          duration.toMillis shouldBe dur.toMillis +- 50L
      }
    }
  }

  "delay" - {

    "100 millis success" in {
      val fa: Future[Int] = Future.successful(99)
      Futil.timed(Futil.delay(100.millis)(fa)).flatMap {
        case (i, duration) =>
          i shouldBe 99
          duration.toMillis shouldBe 100L +- 50L
      }
    }

    "100 millis failure" in {
      case class Expected() extends Throwable
      val fa: Future[Int] = Future.failed(Expected())
      val f = recoverToExceptionIf[Expected](Futil.delay(100.millis)(fa))
      Futil.timed(f).flatMap {
        case (ex, dur) =>
          ex shouldBe Expected()
          dur.toMillis shouldBe 100L +- 50L
      }
    }

  }

  "deadline" - {

    "immediate success" in {
      val fa: Future[Int] = Future.successful(99)
      val f = Futil.deadline(100.millis)(fa)
      Futil.timed(f).flatMap {
        case (i, duration) =>
          i shouldBe 99
          duration.toMillis shouldBe <(20L)
      }
    }

    "immediate failure" in {
      case class Expected() extends Throwable
      val fa: Future[Int] = Future.failed(Expected())
      val f = recoverToExceptionIf[Expected](Futil.deadline(100.millis)(fa))
      Futil.timed(f).flatMap {
        case (ex, duration) =>
          ex shouldBe Expected()
          duration.toMillis shouldBe <(20L)
      }
    }

    "success exceeds deadline" in {
      val fa: Future[Int] = Future { Thread.sleep(200); 99 }
      val f = recoverToExceptionIf[TimeoutException](Futil.deadline(100.millis)(fa))
      Futil.timed(f).flatMap {
        case (exception, duration) =>
          exception.getClass shouldEqual classOf[TimeoutException]
          duration.toMillis shouldBe 100L +- 50L
      }
    }

  }

}
