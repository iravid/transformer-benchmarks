# Efficient, stack-safe monad transformers for Scala

After getting to pick John de Goes' brain in LambdaConf Winter Retreat regarding transformer performance in Scala, I played around with some implementations of ideas he had. So thanks John! The techniques in the optimized interpreter are heavily borrowing ideas from the Scalaz 8 IO interpreter.

The purpose of this repository is to benchmark several implementations of the `State` monad in Scala to see if a stack-safe version can be created without too much of a performance penalty.

There are two implementations of the `State` monad here:
- `FreeState`: A free structure representing operations in the `State` monad that are then interpreted
- `State`: A plain implementation formulated around a newtype of `S => (S, A)`

To interpret `FreeState`, two interpreters are included:
- `Interpreter.runIdiomatic`: an interpreter that used idiomatic Scala code - pattern matching, tuples, etc. It is not stack-safe currently; I tried to do the left-to-right bind reassociation trick but I couldn't get `scalac` to apply `@tailrec`, possibly due to changing type parameters between the calls. This code currently is not in the repo but it's pretty similar.
- `Interpreter.runOptimized`: an optimized version that uses ugly, Java-like code, highly inspired by by the Scalaz 8 IO interpreter, and is stack safe (to the best of my knowledge and experimentation). It also has near-zero allocations.

To summarize, these are the implementations that are benchmarked:

| Implementation                                          | Stack-safe | Interpretable |
|---------------------------------------------------------|------------|---------------|
| Idiomatic                                               | No         | Yes           |
| Optimized                                               | Yes        | Yes           |
| Plain                                                   | No         | No            |
| Cats (`StateT` with `Eval` as a stack-safety mechanism) | Yes        | No            |


To run the benchmarks, use this SBT task:
```
sbt jmh:run -i 10 -wi 10 -f1 -t1
```

These are the benchmark results from running on my machine:
```
Benchmark                                             Mode  Cnt      Score      Error  Units
transformers.Benchmarks.effectfulTraversalIdiomatic  thrpt   10  13299.553 ?  540.366  ops/s
transformers.Benchmarks.effectfulTraversalOptimized  thrpt   10   9744.708 ?  692.701  ops/s
transformers.Benchmarks.effectfulTraversalPlain      thrpt   10  10166.210 ?  167.704  ops/s
transformers.Benchmarks.effectfulTraversalCats       thrpt   10   1559.300 ?   47.680  ops/s
transformers.Benchmarks.getSetIdiomatic              thrpt   10  27915.648 ?  368.691  ops/s
transformers.Benchmarks.getSetOptimized              thrpt   10  23789.783 ? 1281.337  ops/s
transformers.Benchmarks.getSetPlain                  thrpt   10  24104.836 ?  496.736  ops/s
transformers.Benchmarks.getSetCats                   thrpt   10   3808.412 ?   58.164  ops/s
transformers.Benchmarks.leftAssociatedBindIdiomatic  thrpt   10  12938.660 ?  224.829  ops/s
transformers.Benchmarks.leftAssociatedBindOptimized  thrpt   10  13214.841 ?  280.577  ops/s
transformers.Benchmarks.leftAssociatedBindPlain      thrpt   10  63531.226 ?  866.285  ops/s
transformers.Benchmarks.leftAssociatedBindCats       thrpt   10  11761.478 ?  262.621  ops/s
```

### Thoughts:
- I had initially expected the `runOptimized` interpreter to be faster than
  `runIdiomatic`, but the overhead of maintaining a stack for continuations
  takes its toll; when profiling the code, the `addFirst` and `pollFirst`
  methods are the 3rd hottest.
  
  I also checked against the stack implemented in Scalaz 8 IO's RTS, and the
  performance was pretty similar.
  
- It's interesting to see how fast the free, idiomatic version is in some of the
  benchmarks. Even faster than plain function composition. Too bad it's not
  stack safe!
  
- The free versions have an additional advantage: they can be interpreted to
  other monads (e.g., to Scalaz 8 IO or Monix Task) and interpreted in terms of
  those monads
  
- This is still not taking into account the monad transformer use case, and I'll
  need to figure it out next. It probably won't be possible to write custom
  interpreters for that without sacrifices (thread-safety, for example).
  
### Next steps:

Figure out how this can be used: 
- I'm going to to implement the `StateT` version for the free structure and write interpreters into Monix Task and Scalaz 8 IO; both of them have `MVar` implementations that can be used for the state.

- Integrate the `Reader` and `Writer` instructions into the algebra, and perhaps support the indexed state version
