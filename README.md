# futil

Zero-dependency utilities for Scala Futures.

## Purpose

Scala's build-in Futures are a good abstraction for concurrent and asynchronous programming, but they also have some quirks. 
Effect systems (a.k.a. `IO` Monads) like those provided by cats-effect, ZIO, monix, etc. add many useful features
for concurrent and asynchronous programming, but they can be difficult to introduce in an established codebase.
This library aims to add some mileage to Scala's Futures without introducing a totally new effect system.

## API

### Timing 

```scala mdoc
val typeError: Int = 42
```

### Parallelism


### Retries