# futil

Zero-dependency utilities for Scala Futures.

## Purpose

Scala's built-in Futures are a good abstraction for concurrent and asynchronous programming, but they have some quirks. 
Effect systems and IO Monads like those provided by cats-effect, ZIO, monix, akka, etc. add many useful features
for concurrent and asynchronous programming, but they can be difficult to introduce in an established codebase.
This library aims to add some mileage to Scala's Futures without introducing a totally new effect system.

## API

### Setup

```scala mdoc
// Typical Scala concurrency imports.
import scala.concurrent._
import duration._

// Generally not a good idea to use this, but good enough for a readme.
import ExecutionContext.Implicits.global

// Futil imports. Some methods require an implicit java.util.Timer.
import futil._
import Futil.Implicits.timer

// Let's pretend this is calling some external web service. 
def callService(i: Int): Future[Int] = Future(i + 1)
```

### Thunks

Scala Futures execute _eagerly_. This means when we define a `val foo: Future[Int] = ...`, it starts running _now_.

To account for this, some methods in `Futil` use a [_thunk_](https://en.wikipedia.org/wiki/Thunk). 
This is just a fancy word for a function from `Unit` to some return type.

For example, a Thunk for a `Future[Int]`:

```scala mdoc
def future(): Future[Int] = thunk()
val thunk: () => Future[Int] = () => future()
val alsoAThunk: () => Future[Int] = Futil.thunk(future())
```

A thunk of a Future is useful in two cases:

1. When we need to delay the execution of a Future.
2. When we need a way to re-run the Future on-demand.

They're not fool-proof. For instance, if you define the Future as a `val`, and _then_ wrap it in a thunk, it will 
silently defeat the purpose of the whole exercise.

### Timing

Time the execution of a Future.

```scala mdoc
// Times the service call, returning the value and the execution duration.
val timed: Future[(Int, Duration)] = Futil.timed(callService(42))
```

Limit the time a Future spends executing.

```scala mdoc
// Returns a failed Future if the service call exceeds the given duration.
val deadline: Future[Int] = Futil.deadline(1.seconds)(callService(42))
```

Delay the execution of a Future.

```scala mdoc
// Waits the given duration before executing the Future.
val delayed: Future[Int] = Futil.delay(1.seconds)(callService(42))
```

### Parallelism


### Retries