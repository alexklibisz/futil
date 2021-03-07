package futil

import org.scalatest.Succeeded
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._

class ThunkSpec extends AsyncFreeSpec with Matchers {

  import Futil.Implicits.timer
  override implicit val executionContext: ExecutionContext = ExecutionContext.global

  "Thunk" - {

    "does not prematurely execute a def" in {
      var i = 0
      def run(): Future[Unit] = Future(i += 1)
      val thunk = Futil.thunk(run())
      for {
        _ <- Futil.delay(100.millis)(Future.successful(()))
        _ = i shouldBe 0
        _ <- thunk().map(_ => i shouldBe 1)
      } yield Succeeded
    }

    "does not prematurely execute a lazy val" in {
      var i = 0
      lazy val run: Future[Unit] = Future(i += 1)
      val thunk = Futil.thunk(run)
      for {
        _ <- Futil.delay(100.millis)(Future.successful(()))
        _ = i shouldBe 0
        _ <- thunk().map(_ => i shouldBe 1)
      } yield Succeeded
    }

    "does prematurely execute a regular val" in {
      var i = 0
      val run: Future[Unit] = Future(i += 1)
      val thunk = Futil.thunk(run)
      for {
        _ <- Futil.delay(100.millis)(Future.successful(()))
        _ = i shouldBe 1
        _ <- thunk().map(_ => i shouldBe 1)
      } yield Succeeded
    }

  }

}
