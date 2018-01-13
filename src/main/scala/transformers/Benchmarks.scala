package transformers

import java.util.concurrent.TimeUnit
import org.openjdk.jmh.annotations._
import cats.data.{ State => CatsState }

@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.SECONDS)
class Benchmarks {
  @Benchmark
  def leftAssociatedBindOptimized(): Int = {
    def loop(i: Int): FreeState[Int, Int] =
      if (i > 10000) FreeState.pure(i)
      else FreeState.pure(i + 1).flatMap(loop)

    Interpreter.runOptimized(0)(FreeState.pure(0).flatMap(loop))._1
  }

  @Benchmark
  def leftAssociatedBindIdiomatic(): Int = {
    def loop(i: Int): FreeState[Int, Int] =
      if (i > 10000) FreeState.pure(i)
      else FreeState.pure(i + 1).flatMap(loop)

    Interpreter.runIdiomatic(0)(FreeState.pure(0).flatMap(loop))._1
  }

  @Benchmark
  def leftAssociatedBindPlain(): Int = {
    def loop(i: Int): State[Int, Int] =
      if (i > 1000) State.pure(i)
      else State.pure(i + 1).flatMap(loop)

    State.pure(0).flatMap(loop).run(0)._1
  }

  @Benchmark
  def leftAssociatedBindCats(): Int = {
    def loop(i: Int): CatsState[Int, Int] =
      if (i > 1000) CatsState.pure(i)
      else CatsState.pure(i + 1).flatMap(loop)

    CatsState.pure(0).flatMap(loop).run(0).value._1
  }

  @Benchmark
  def getSetOptimized(): Int = {
    def loop(i: Int, acc: FreeState[Int, Int]): FreeState[Int, Int] =
      if (i > 1000) acc.flatMap(_ => FreeState.set(i)).flatMap(_ => FreeState.get)
      else loop(i + 1, acc.flatMap(_ => FreeState.set(i)).flatMap(_ => FreeState.get))

    Interpreter.runOptimized(0)(loop(0, FreeState.pure(0)))._1
  }

  @Benchmark
  def getSetIdiomatic(): Int = {
    def loop(i: Int, acc: FreeState[Int, Int]): FreeState[Int, Int] =
      if (i > 1000) acc.flatMap(_ => FreeState.set(i)).flatMap(_ => FreeState.get)
      else loop(i + 1, acc.flatMap(_ => FreeState.set(i)).flatMap(_ => FreeState.get))

    Interpreter.runIdiomatic(0)(loop(0, FreeState.pure(0)))._1
  }

  @Benchmark
  def getSetPlain(): Int = {
    def loop(i: Int, acc: State[Int, Int]): State[Int, Int] =
      if (i > 1000) acc.flatMap(_ => State.set(i)).flatMap(_ => State.get)
      else loop(i + 1, acc.flatMap(_ => State.set(i)).flatMap(_ => State.get))

    loop(0, State.pure(0)).run(0)._1
  }

  @Benchmark
  def getSetCats(): Int = {
    def loop(i: Int, acc: CatsState[Int, Int]): CatsState[Int, Int] =
      if (i > 1000) acc.flatMap(_ => CatsState.set(i)).flatMap(_ => CatsState.get)
      else loop(i + 1, acc.flatMap(_ => CatsState.set(i)).flatMap(_ => CatsState.get))

    loop(0, CatsState.pure(0)).run(0).value._1
  }

  @Benchmark
  def effectfulTraversalIdiomatic(): Int =
    Interpreter.runIdiomatic(0) {
      FreeState.traverse((0 to 1000).toList) { el =>
        FreeState.get[Int].flatMap(s => FreeState.set(s + el))
      }
    }._1

  @Benchmark
  def effectfulTraversalOptimized(): Int =
    Interpreter.runOptimized(0) {
      FreeState.traverse((0 to 1000).toList) { el =>
        FreeState.get[Int].flatMap(s => FreeState.set(s + el))
      }
    }._1

  @Benchmark
  def effectfulTraversalPlain(): Int =
    State.traverse((0 to 1000).toList) { el =>
      State.get[Int].flatMap(s => State.set(s + el))
    }.run(0)._1

  @Benchmark
  def effectfulTraversalCats(): Int = {
    import cats.implicits._

    (0 to 1000).toList.traverse { el =>
      CatsState.get[Int].flatMap(s => CatsState.set(s + el))
    }.run(0).value._1
  }
}
