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
| Scalaz 7.3.x                                            | Yes        | Yes           |


To run the benchmarks, use this SBT task:
```
sbt jmh:run -i 10 -wi 10 -f1 -t1
```

The benchmarks run each benchmark with 1k, 10k, 100k and 1mil elements against each implementation. Some benchmarks are missing due to stack safety issues.

These are the results from running on my machin:
```
Benchmark                                   Mode   Cnt     Score     Error  Units
Benchmarks.effectfulTraversalIdiomatic1k    thrpt   10  13604.397 ?  257.137  ops/s
Benchmarks.effectfulTraversalOptimized1k    thrpt   10  10510.265 ?  242.085  ops/s
Benchmarks.effectfulTraversalPlain1k        thrpt   10  10919.611 ?  632.883  ops/s
Benchmarks.effectfulTraversalCats1k         thrpt   10   1637.210 ?   39.364  ops/s
Benchmarks.effectfulTraversalScalaz1k       thrpt   10   2152.315 ?  29.411  ops/s

Benchmarks.effectfulTraversalIdiomatic10k   [crashed]
Benchmarks.effectfulTraversalOptimized10k   thrpt   10   780.790 ?  16.632  ops/s
Benchmarks.effectfulTraversalPlain10k       [crashed]
Benchmarks.effectfulTraversalCats10k        thrpt   10   118.983 ?   1.942  ops/s
Benchmarks.effectfulTraversalScalaz10k      thrpt   10   190.752 ?   5.601  ops/s

Benchmarks.effectfulTraversalIdiomatic100k  [crashed]
Benchmarks.effectfulTraversalOptimized100k  thrpt   10    84.006 ?   3.444  ops/s
Benchmarks.effectfulTraversalPlain100k      [crashed]
Benchmarks.effectfulTraversalCats100k       thrpt   10     9.270 ?   0.747  ops/s
Benchmarks.effectfulTraversalScalaz100k     thrpt   10    14.482 ?   1.237  ops/s

Benchmarks.effectfulTraversalIdiomatic1mil  [crashed]
Benchmarks.effectfulTraversalOptimized1mil  thrpt   10     7.443 ?   0.879  ops/s
Benchmarks.effectfulTraversalPlain1mil      [crashed]
Benchmarks.effectfulTraversalCats1mil       thrpt   10     0.256 ?   0.091  ops/s
Benchmarks.effectfulTraversalScalaz1mil     thrpt   10     0.329 ?   0.109  ops/s

Benchmarks.getSetIdiomatic1k                thrpt   10  29697.034 ?  758.154  ops/s
Benchmarks.getSetOptimized1k                thrpt   10  24496.856 ?  674.744  ops/s
Benchmarks.getSetPlain1k                    thrpt   10  24262.003 ?  574.330  ops/s
Benchmarks.getSetCats1k                     thrpt   10   4078.638 ?   83.634  ops/s
Benchmarks.getSetScalaz1k                   thrpt   10   9298.865 ? 314.259  ops/s

Benchmarks.getSetIdiomatic10k               [crashed]
Benchmarks.getSetOptimized10k               thrpt   10  2376.048 ?  61.998  ops/s
Benchmarks.getSetPlain10k                   [crashed]
Benchmarks.getSetCats10k                    [crashed]
Benchmarks.getSetScalaz10k                  thrpt   10   739.830 ?  71.689  ops/s

Benchmarks.getSetIdiomatic100k              [crashed]
Benchmarks.getSetOptimized100k              thrpt   10   230.924 ?   6.935  ops/s
Benchmarks.getSetPlain100k                  [crashed]
Benchmarks.getSetCats100k                   [crashed]
Benchmarks.getSetScalaz100k                 thrpt   10    70.382 ?   2.429  ops/s

Benchmarks.getSetIdiomatic1mil              [crashed]
Benchmarks.getSetOptimized1mil              thrpt   10    17.981 ?   5.222  ops/s
Benchmarks.getSetPlain1mil                  [crashed]
Benchmarks.getSetCats1mil                   [crashed]
Benchmarks.getSetScalaz1mil                 thrpt   10     3.228 ?   0.849  ops/s

Benchmarks.leftAssociatedBindIdiomatic1k    thrpt   10  86726.408 ? 2028.591  ops/s
Benchmarks.leftAssociatedBindOptimized1k    thrpt   10  99862.966 ? 1662.192  ops/s
Benchmarks.leftAssociatedBindPlain1k        thrpt   10  53196.438 ? 1132.752  ops/s
Benchmarks.leftAssociatedBindCats1k         thrpt   10  11630.047 ?  264.939  ops/s
Benchmarks.leftAssociatedBindScalaz1k       thrpt   10  22804.412 ? 944.732  ops/s

Benchmarks.leftAssociatedBindIdiomatic10k   thrpt   10  9743.492 ? 119.160  ops/s
Benchmarks.leftAssociatedBindOptimized10k   thrpt   10  8276.080 ? 202.169  ops/s
Benchmarks.leftAssociatedBindPlain10k       [crashed]
Benchmarks.leftAssociatedBindCats10k        thrpt   10  1145.639 ?  17.292  ops/s
Benchmarks.leftAssociatedBindScalaz10k      thrpt   10  2278.677 ?  31.877  ops/s

Benchmarks.leftAssociatedBindIdiomatic100k  thrpt   10   970.573 ?   9.293  ops/s
Benchmarks.leftAssociatedBindOptimized100k  thrpt   10   848.888 ?  10.699  ops/s
Benchmarks.leftAssociatedBindPlain100k      [crashed]
Benchmarks.leftAssociatedBindCats100k       thrpt   10   112.690 ?   1.472  ops/s
Benchmarks.leftAssociatedBindScalaz100k     thrpt   10   223.351 ?  10.576  ops/s

Benchmarks.leftAssociatedBindIdiomatic1mil  thrpt   10    95.518 ?   4.284  ops/s
Benchmarks.leftAssociatedBindOptimized1mil  thrpt   10    84.350 ?   2.080  ops/s
Benchmarks.leftAssociatedBindPlain1mil      [crashed]
Benchmarks.leftAssociatedBindCats1mil       thrpt   10    11.455 ?   0.352  ops/s
Benchmarks.leftAssociatedBindScalaz1mil     thrpt   10    21.228 ?   1.803  ops/s
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
