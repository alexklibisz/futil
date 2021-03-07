package futil

import org.scalatest.AsyncTestSuite

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}

trait GlobalExecutionContext { this: AsyncTestSuite =>

  override implicit val executionContext: ExecutionContextExecutor = ExecutionContext.global

}
