package futil

import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers

import scala.concurrent.duration._
import scala.concurrent._
import org.scalatest.Succeeded

class AsyncSemaphoreSpec extends AsyncFreeSpec with GlobalExecutionContext with Matchers {

  import Futil._
  import Futil.Implicits.timer

  "AsyncSemaphore" - {

    "at least 1 permit" in {
      val ex = intercept[IllegalArgumentException](semaphore(0))
      Future(ex.getMessage shouldBe "requirement failed: semaphore must have at least 1 permit")
    }

    "acquire immediately when permits are available" in {
      val s = semaphore(1)
      timed(s.acquire()).map {
        case (_, dur) => dur.toMillis shouldBe <(50L)
      }
    }

    "prevent acquiring when permits are exhausted" in {
      val s = semaphore(2)
      val check = for {
        _ <- s.acquire()
        _ <- s.acquire()
        _ <- recoverToSucceededIf[TimeoutException](deadline(500.millis)(s.acquire()))
        _ <- s.release()
        _ <- s.release()
      } yield Succeeded
      deadline(600.millis)(check)
    }

    "releasing when no permits are acquired is a no-op" in {
      val s = semaphore(2)
      for {
        _ <- s.release()
        _ <- s.release()
        (a, w) <- s.inspect()
      } yield (a, w) shouldBe (2, 0)
    }

    "1 million async-bound Futures" in {
      val s = semaphore(2)

      def keep(dur: Duration): Future[Unit] =
        for {
          _ <- s.acquire()
          _ <- delay(dur)(Future.successful(()))
          _ <- s.release()
        } yield ()

      val as = (0 to 999999).toVector
      val waiting = as.map(_ => keep(10000.nanos))

      for {
        (_, dur) <- deadline(60.seconds)(timed(Future.sequence(waiting)))
        (a, w) <- s.inspect()
      } yield {
        info(s"Completed ${as.length} tasks in ${dur.toSeconds.seconds}")
        dur.toSeconds shouldBe <(20L) // TODO: this takes 2 seconds locally and 15 in GH actions.
        (a, w) shouldBe (2, 0)
      }
    }

    "acquire/release has a risk of starvation if failures are not handled properly" in {
      val s = semaphore(2)
      val check = for {
        _ <- s.acquire()
        _ <- s.acquire()
        // Future fails and does not release.
        _ = delay(100.millis)(Future.failed(new Exception)).flatMap(_ => s.release())
        // Acquiring again will hang indefinitely.
        _ <- s.acquire()
      } yield ()

      recoverToSucceededIf[TimeoutException](Futil.deadline(500.millis)(check))
    }

    "withPermit correctly handles failures" in {
      val s = semaphore(2)
      val check = for {
        _ <- s.acquire()
        _ = s.withPermit(() => delay(100.millis)(Future.failed(new Exception)))
        _ <- s.acquire()
      } yield Succeeded
      deadline(200.millis)(check)
    }

  }

}
