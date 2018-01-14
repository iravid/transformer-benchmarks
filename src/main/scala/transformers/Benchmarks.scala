package transformers

import java.util.concurrent.TimeUnit
import org.openjdk.jmh.annotations._
import cats.data.{ State => CatsState }
import scalaz.{ State => ScalazState }

@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.SECONDS)
class Benchmarks {
  def leftAssociatedBindOptimized(bound: Int): Int = {
    def loop(i: Int): FreeState[Int, Int] =
      if (i > bound) FreeState.pure(i)
      else FreeState.pure(i + 1).flatMap(loop)

    Interpreter.runOptimized(0)(FreeState.pure(0).flatMap(loop))._1
  }

  def leftAssociatedBindIdiomatic(bound: Int): Int = {
    def loop(i: Int): FreeState[Int, Int] =
      if (i > bound) FreeState.pure(i)
      else FreeState.pure(i + 1).flatMap(loop)

    Interpreter.runIdiomatic(0)(FreeState.pure(0).flatMap(loop))._1
  }

  def leftAssociatedBindIdiomaticRec(bound: Int): Int = {
    def loop(i: Int): FreeState[Int, Int] =
      if (i > bound) FreeState.pure(i)
      else FreeState.pure(i + 1).flatMap(loop)

    Interpreter.runIdiomaticRec(0)(FreeState.pure(0).flatMap(loop))._1
  }

  def leftAssociatedBindPlain(bound: Int): Int = {
    def loop(i: Int): State[Int, Int] =
      if (i > bound) State.pure(i)
      else State.pure(i + 1).flatMap(loop)

    State.pure(0).flatMap(loop).run(0)._1
  }

  def leftAssociatedBindCats(bound: Int): Int = {
    def loop(i: Int): CatsState[Int, Int] =
      if (i > bound) CatsState.pure(i)
      else CatsState.pure(i + 1).flatMap(loop)

    CatsState.pure(0).flatMap(loop).run(0).value._1
  }

  def leftAssociatedBindScalaz(bound: Int): Int = {
    def loop(i: Int): ScalazState[Int, Int] =
      if (i > bound) ScalazState.state(i)
      else ScalazState.state(i + 1).flatMap(loop)

    ScalazState.state(0).flatMap(loop).runRec(0)._1
  }

  def getSetOptimized(bound: Int): Int = {
    def loop(i: Int, acc: FreeState[Int, Int]): FreeState[Int, Int] =
      if (i > bound) acc.flatMap(_ => FreeState.set(i)).flatMap(_ => FreeState.get)
      else loop(i + 1, acc.flatMap(_ => FreeState.set(i)).flatMap(_ => FreeState.get))

    Interpreter.runOptimized(0)(loop(0, FreeState.pure(0)))._1
  }

  def getSetIdiomatic(bound: Int): Int = {
    def loop(i: Int, acc: FreeState[Int, Int]): FreeState[Int, Int] =
      if (i > bound) acc.flatMap(_ => FreeState.set(i)).flatMap(_ => FreeState.get)
      else loop(i + 1, acc.flatMap(_ => FreeState.set(i)).flatMap(_ => FreeState.get))

    Interpreter.runIdiomatic(0)(loop(0, FreeState.pure(0)))._1
  }

  def getSetIdiomaticRec(bound: Int): Int = {
    def loop(i: Int, acc: FreeState[Int, Int]): FreeState[Int, Int] =
      if (i > bound) acc.flatMap(_ => FreeState.set(i)).flatMap(_ => FreeState.get)
      else loop(i + 1, acc.flatMap(_ => FreeState.set(i)).flatMap(_ => FreeState.get))

    Interpreter.runIdiomaticRec(0)(loop(0, FreeState.pure(0)))._1
  }

  def getSetPlain(bound: Int): Int = {
    def loop(i: Int, acc: State[Int, Int]): State[Int, Int] =
      if (i > bound) acc.flatMap(_ => State.set(i)).flatMap(_ => State.get)
      else loop(i + 1, acc.flatMap(_ => State.set(i)).flatMap(_ => State.get))

    loop(0, State.pure(0)).run(0)._1
  }

  def getSetCats(bound: Int): Int = {
    def loop(i: Int, acc: CatsState[Int, Int]): CatsState[Int, Int] =
      if (i > bound) acc.flatMap(_ => CatsState.set(i)).flatMap(_ => CatsState.get)
      else loop(i + 1, acc.flatMap(_ => CatsState.set(i)).flatMap(_ => CatsState.get))

    loop(0, CatsState.pure(0)).run(0).value._1
  }

  def getSetScalaz(bound: Int): Int = {
    def loop(i: Int, acc: ScalazState[Int, Int]): ScalazState[Int, Int] =
      if (i > bound) acc.flatMap(_ => ScalazState.put(i)).flatMap(_ => ScalazState.get)
      else loop(i + 1, acc.flatMap(_ => ScalazState.put(i)).flatMap(_ => ScalazState.get))

    loop(0, ScalazState.state(0)).runRec(0)._1
  }

  def effectfulTraversalIdiomatic(bound: Int): Int =
    Interpreter.runIdiomatic(0) {
      FreeState.traverse((0 to bound).toList) { el =>
        FreeState.get[Int].flatMap(s => FreeState.set(s + el))
      }
    }._1

  def effectfulTraversalIdiomaticRec(bound: Int): Int =
    Interpreter.runIdiomaticRec(0) {
      FreeState.traverse((0 to bound).toList) { el =>
        FreeState.get[Int].flatMap(s => FreeState.set(s + el))
      }
    }._1

  def effectfulTraversalOptimized(bound: Int): Int =
    Interpreter.runOptimized(0) {
      FreeState.traverse((0 to bound).toList) { el =>
        FreeState.get[Int].flatMap(s => FreeState.set(s + el))
      }
    }._1

  def effectfulTraversalPlain(bound: Int): Int =
    State.traverse((0 to bound).toList) { el =>
      State.get[Int].flatMap(s => State.set(s + el))
    }.run(0)._1

  def effectfulTraversalCats(bound: Int): Int = {
    import cats.instances.list._, cats.syntax.traverse._

    (0 to bound).toList.traverse { el =>
      CatsState.get[Int].flatMap(s => CatsState.set(s + el))
    }.run(0).value._1
  }

  def effectfulTraversalScalaz(bound: Int): Int = {
    import scalaz.std.list._, scalaz.syntax.traverse._

    (0 to bound).toList.traverse { el =>
      ScalazState.get[Int].flatMap(s => ScalazState.put(s + el))
    }.runRec(0)._1
  }

  def wrapFunOptimized(bound: Int): Int = {
    def loop(i: Int, acc: FreeState[Int, Int]): FreeState[Int, Int] =
      if (i > bound) acc
      else loop(i + 1, acc.flatMap(_ => FreeState(i => (i+1, i+1))))

    Interpreter.runOptimized(0)(loop(0, FreeState.pure(0)))._1
  }

  def wrapFunIdiomatic(bound: Int): Int = {
    def loop(i: Int, acc: FreeState[Int, Int]): FreeState[Int, Int] =
      if (i > bound) acc
      else loop(i + 1, acc.flatMap(_ => FreeState(i => (i+1, i+1))))

    Interpreter.runIdiomatic(0)(loop(0, FreeState.pure(0)))._1
  }

  def wrapFunIdiomaticRec(bound: Int): Int = {
    def loop(i: Int, acc: FreeState[Int, Int]): FreeState[Int, Int] =
      if (i > bound) acc
      else loop(i + 1, acc.flatMap(_ => FreeState(i => (i+1, i+1))))

    Interpreter.runIdiomaticRec(0)(loop(0, FreeState.pure(0)))._1
  }

  def wrapFunPlain(bound: Int): Int = {
    def loop(i: Int, acc: State[Int, Int]): State[Int, Int] =
      if (i > bound) acc
      else loop(i + 1, acc.flatMap(_ => State(i => (i+1, i+1))))

    loop(0, State.pure(0)).run(0)._1
  }

  def wrapFunCats(bound: Int): Int = {
    def loop(i: Int, acc: CatsState[Int, Int]): CatsState[Int, Int] =
      if (i > bound) acc
      else loop(i + 1, acc.flatMap(_ => CatsState(i => (i+1, i+1))))

    loop(0, CatsState.pure(0)).run(0).value._1
  }

  def wrapFunScalaz(bound: Int): Int = {
    def loop(i: Int, acc: ScalazState[Int, Int]): ScalazState[Int, Int] =
      if (i > bound) acc
      else loop(i + 1, acc.flatMap(_ => ScalazState(i => (i+1, i+1))))

    loop(0, ScalazState.state(0)).runRec(0)._1
  }

  @Benchmark
  def effectfulTraversalIdiomatic1k(): Int = effectfulTraversalIdiomatic(1000)
  @Benchmark
  def effectfulTraversalIdiomatic10k(): Int = effectfulTraversalIdiomatic(10000)
  @Benchmark
  def effectfulTraversalIdiomatic100k(): Int = effectfulTraversalIdiomatic(100000)
  @Benchmark
  def effectfulTraversalIdiomatic1mil(): Int = effectfulTraversalIdiomatic(1000000)

  @Benchmark
  def getSetIdiomatic1k(): Int = getSetIdiomatic(1000)
  @Benchmark
  def getSetIdiomatic10k(): Int = getSetIdiomatic(10000)
  @Benchmark
  def getSetIdiomatic100k(): Int = getSetIdiomatic(100000)
  @Benchmark
  def getSetIdiomatic1mil(): Int = getSetIdiomatic(1000000)
  
  @Benchmark
  def leftAssociatedBindIdiomatic1k(): Int = leftAssociatedBindIdiomatic(1000)
  @Benchmark
  def leftAssociatedBindIdiomatic10k(): Int = leftAssociatedBindIdiomatic(10000)
  @Benchmark
  def leftAssociatedBindIdiomatic100k(): Int = leftAssociatedBindIdiomatic(100000)
  @Benchmark
  def leftAssociatedBindIdiomatic1mil(): Int = leftAssociatedBindIdiomatic(1000000)

  @Benchmark
  def wrapFunIdiomatic1k(): Int = wrapFunIdiomatic(1000)
  @Benchmark
  def wrapFunIdiomatic10k(): Int = wrapFunIdiomatic(10000)
  @Benchmark
  def wrapFunIdiomatic100k(): Int = wrapFunIdiomatic(100000)
  @Benchmark
  def wrapFunIdiomatic1mil(): Int = wrapFunIdiomatic(1000000)

  @Benchmark
  def effectfulTraversalIdiomaticRec1k(): Int = effectfulTraversalIdiomaticRec(1000)
  @Benchmark
  def effectfulTraversalIdiomaticRec10k(): Int = effectfulTraversalIdiomaticRec(10000)
  @Benchmark
  def effectfulTraversalIdiomaticRec100k(): Int = effectfulTraversalIdiomaticRec(100000)
  @Benchmark
  def effectfulTraversalIdiomaticRec1mil(): Int = effectfulTraversalIdiomaticRec(1000000)

  @Benchmark
  def getSetIdiomaticRec1k(): Int = getSetIdiomaticRec(1000)
  @Benchmark
  def getSetIdiomaticRec10k(): Int = getSetIdiomaticRec(10000)
  @Benchmark
  def getSetIdiomaticRec100k(): Int = getSetIdiomaticRec(100000)
  @Benchmark
  def getSetIdiomaticRec1mil(): Int = getSetIdiomaticRec(1000000)

  @Benchmark
  def leftAssociatedBindIdiomaticRec1k(): Int = leftAssociatedBindIdiomaticRec(1000)
  @Benchmark
  def leftAssociatedBindIdiomaticRec10k(): Int = leftAssociatedBindIdiomaticRec(10000)
  @Benchmark
  def leftAssociatedBindIdiomaticRec100k(): Int = leftAssociatedBindIdiomaticRec(100000)
  @Benchmark
  def leftAssociatedBindIdiomaticRec1mil(): Int = leftAssociatedBindIdiomaticRec(1000000)

  @Benchmark
  def wrapFunIdiomaticRec1k(): Int = wrapFunIdiomaticRec(1000)
  @Benchmark
  def wrapFunIdiomaticRec10k(): Int = wrapFunIdiomaticRec(10000)
  @Benchmark
  def wrapFunIdiomaticRec100k(): Int = wrapFunIdiomaticRec(100000)
  @Benchmark
  def wrapFunIdiomaticRec1mil(): Int = wrapFunIdiomaticRec(1000000)

  @Benchmark
  def effectfulTraversalOptimized1k(): Int = effectfulTraversalOptimized(1000)
  @Benchmark
  def effectfulTraversalOptimized10k(): Int = effectfulTraversalOptimized(10000)
  @Benchmark
  def effectfulTraversalOptimized100k(): Int = effectfulTraversalOptimized(100000)
  @Benchmark
  def effectfulTraversalOptimized1mil(): Int = effectfulTraversalOptimized(1000000)

  @Benchmark
  def getSetOptimized1k(): Int = getSetOptimized(1000)
  @Benchmark
  def getSetOptimized10k(): Int = getSetOptimized(10000)
  @Benchmark
  def getSetOptimized100k(): Int = getSetOptimized(100000)
  @Benchmark
  def getSetOptimized1mil(): Int = getSetOptimized(1000000)
  
  @Benchmark
  def leftAssociatedBindOptimized1k(): Int = leftAssociatedBindOptimized(1000)
  @Benchmark
  def leftAssociatedBindOptimized10k(): Int = leftAssociatedBindOptimized(10000)
  @Benchmark
  def leftAssociatedBindOptimized100k(): Int = leftAssociatedBindOptimized(100000)
  @Benchmark
  def leftAssociatedBindOptimized1mil(): Int = leftAssociatedBindOptimized(1000000)

  @Benchmark
  def wrapFunOptimized1k():  Int = wrapFunOptimized(1000)
  @Benchmark
  def wrapFunOptimized10k():  Int = wrapFunOptimized(10000)
  @Benchmark
  def wrapFunOptimized100k(): Int = wrapFunOptimized(100000)
  @Benchmark
  def wrapFunOptimized1mil(): Int = wrapFunOptimized(1000000)

  @Benchmark
  def effectfulTraversalPlain1k(): Int = effectfulTraversalPlain(1000)
  @Benchmark
  def effectfulTraversalPlain10k(): Int = effectfulTraversalPlain(10000)
  @Benchmark
  def effectfulTraversalPlain100k(): Int = effectfulTraversalPlain(100000)
  @Benchmark
  def effectfulTraversalPlain1mil(): Int = effectfulTraversalPlain(1000000)

  @Benchmark
  def getSetPlain1k(): Int = getSetPlain(1000)
  @Benchmark
  def getSetPlain10k(): Int = getSetPlain(10000)
  @Benchmark
  def getSetPlain100k(): Int = getSetPlain(100000)
  @Benchmark
  def getSetPlain1mil(): Int = getSetPlain(1000000)
  
  @Benchmark
  def leftAssociatedBindPlain1k(): Int = leftAssociatedBindPlain(1000)
  @Benchmark
  def leftAssociatedBindPlain10k(): Int = leftAssociatedBindPlain(10000)
  @Benchmark
  def leftAssociatedBindPlain100k(): Int = leftAssociatedBindPlain(100000)
  @Benchmark
  def leftAssociatedBindPlain1mil(): Int = leftAssociatedBindPlain(1000000)

  @Benchmark
  def wrapFunPlain1k(): Int = wrapFunPlain(1000)
  @Benchmark
  def wrapFunPlain10k(): Int = wrapFunPlain(10000)
  @Benchmark
  def wrapFunPlain100k(): Int = wrapFunPlain(100000)
  @Benchmark
  def wrapFunPlain1mil(): Int = wrapFunPlain(1000000)

  @Benchmark
  def effectfulTraversalCats1k(): Int = effectfulTraversalCats(1000)
  @Benchmark
  def effectfulTraversalCats10k(): Int = effectfulTraversalCats(10000)
  @Benchmark
  def effectfulTraversalCats100k(): Int = effectfulTraversalCats(100000)
  @Benchmark
  def effectfulTraversalCats1mil(): Int = effectfulTraversalCats(1000000)

  @Benchmark
  def getSetCats1k(): Int = getSetCats(1000)
  @Benchmark
  def getSetCats10k(): Int = getSetCats(10000)
  @Benchmark
  def getSetCats100k(): Int = getSetCats(100000)
  @Benchmark
  def getSetCats1mil(): Int = getSetCats(1000000)
  
  @Benchmark
  def leftAssociatedBindCats1k(): Int = leftAssociatedBindCats(1000)
  @Benchmark
  def leftAssociatedBindCats10k(): Int = leftAssociatedBindCats(10000)
  @Benchmark
  def leftAssociatedBindCats100k(): Int = leftAssociatedBindCats(100000)
  @Benchmark
  def leftAssociatedBindCats1mil(): Int = leftAssociatedBindCats(1000000)

  @Benchmark
  def effectfulTraversalScalaz1k(): Int = effectfulTraversalScalaz(1000)
  @Benchmark
  def effectfulTraversalScalaz10k(): Int = effectfulTraversalScalaz(10000)
  @Benchmark
  def effectfulTraversalScalaz100k(): Int = effectfulTraversalScalaz(100000)
  @Benchmark
  def effectfulTraversalScalaz1mil(): Int = effectfulTraversalScalaz(1000000)

  @Benchmark
  def getSetScalaz1k(): Int = getSetScalaz(1000)
  @Benchmark
  def getSetScalaz10k(): Int = getSetScalaz(10000)
  @Benchmark
  def getSetScalaz100k(): Int = getSetScalaz(100000)
  @Benchmark
  def getSetScalaz1mil(): Int = getSetScalaz(1000000)
  
  @Benchmark
  def leftAssociatedBindScalaz1k(): Int = leftAssociatedBindScalaz(1000)
  @Benchmark
  def leftAssociatedBindScalaz10k(): Int = leftAssociatedBindScalaz(10000)
  @Benchmark
  def leftAssociatedBindScalaz100k(): Int = leftAssociatedBindScalaz(100000)
  @Benchmark
  def leftAssociatedBindScalaz1mil(): Int = leftAssociatedBindScalaz(1000000)

  @Benchmark
  def wrapFunScalaz1k(): Int = wrapFunScalaz(1000)
  @Benchmark
  def wrapFunScalaz10k(): Int = wrapFunScalaz(10000)
  @Benchmark
  def wrapFunScalaz100k(): Int = wrapFunScalaz(100000)
  @Benchmark
  def wrapFunScalaz1mil(): Int = wrapFunScalaz(1000000)
}
