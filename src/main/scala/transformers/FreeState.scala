package transformers

sealed abstract class FreeState[S, A] { self =>
  def tag: Int

  final def map[B](f: A => B): FreeState[S, B] = FreeState.FlatMap(self, f.andThen(FreeState.Pure(_)))

  final def flatMap[B](f: A => FreeState[S, B]): FreeState[S, B] =
    FreeState.FlatMap(self, f)
}

object FreeState {
  final def pure[S, A](a: A): FreeState[S, A] = Pure(a)

  final def get[S]: FreeState[S, S] = FreeState.Get()

  final def set[S](s: S): FreeState[S, Unit] = FreeState.Set(s)

  def traverse[S, A, B](l: List[A])(f: A => FreeState[S, B]): FreeState[S, List[B]] =
    l.foldLeft(pure[S, List[B]](Nil: List[B])) { (acc, el) =>
      acc flatMap { bs =>
        f(el) map { b =>
          b :: bs
        }
      }
    } map (_.reverse)

  object Tags {
    final val Pure = 0
    final val FlatMap = 1
    final val Map = 2
    final val Get = 3
    final val Set = 4
  }

  final case class Pure[S, A](a: A) extends FreeState[S, A] {
    override final def tag = Tags.Pure
  }

  final case class FlatMap[S, A, B](fa: FreeState[S, A], f: A => FreeState[S, B]) 
      extends FreeState[S, B] {
    override final def tag = Tags.FlatMap
  }

  final case class Get[S]() extends FreeState[S, S] {
    override final def tag = Tags.Get
  }

  final case class Set[S](s: S) extends FreeState[S, Unit] {
    override final def tag = Tags.Set
  }
}

object Interpreter {
  def runOptimized[S, A](init: S)(fa: FreeState[S, A]): (S, A) = {
    var currOp: FreeState[S, Any] = fa.asInstanceOf[FreeState[S, Any]]
    var done: Boolean = false

    var conts: java.util.ArrayDeque[Any => FreeState[S, Any]] = new java.util.ArrayDeque

    var state: S = init
    var res: Any = null

    // Put () on the stack to avoid field access
    val unit = ()

    do {
      currOp.tag match {
        case FreeState.Tags.Pure =>
          res = currOp.asInstanceOf[FreeState.Pure[S, A]].a

          if (conts.isEmpty())
            done = true
          else
            currOp = conts.pollFirst()(res)

        case FreeState.Tags.Get =>
          res = state

          if (conts.isEmpty())
            done = true
          else
            currOp = conts.pollFirst()(res)

        case FreeState.Tags.Set =>
          val op = currOp.asInstanceOf[FreeState.Set[S]]

          state = op.s
          res = unit

          if (conts.isEmpty())
            done = true
          else
            currOp = conts.pollFirst()(res)

        case FreeState.Tags.FlatMap =>
          val op = currOp.asInstanceOf[FreeState.FlatMap[S, Any, Any]]

          op.fa.tag match {
            case FreeState.Tags.Pure =>
              val nested = op.fa.asInstanceOf[FreeState.Pure[S, Any]]

              res = nested.a
              currOp = op.f(nested.a)

            case FreeState.Tags.Get =>
              res = state
              currOp = op.f(state)

            case FreeState.Tags.Set =>
              val nested = op.fa.asInstanceOf[FreeState.Set[S]]

              state = nested.s
              res = unit
              currOp = op.f(unit)

            case _ =>
              currOp = op.fa
              conts.addFirst(op.f)
          }
      }
    } while (!done)

    (state, res.asInstanceOf[A])
  }

  // GADT shenanigans. This should be at some point the stack safe version.
  // def runIdiomatic[S, A](s0: S)(fa0: FreeState[S, A]): (S, A) = {
  //   @annotation.tailrec
  //   def go(s: S, fa: FreeState[S, A]): (S, A) =
  //     fa match {
  //       case FreeState.Pure(a) =>
  //         (s, a)

  //       case FreeState.FlatMap(fa, f) =>
  //         fa match {
  //           case FreeState.Pure(a) =>
  //             go(s, f(a))

  //           case FreeState.Get() =>
  //             go(s, f(s))

  //           case x: FreeState.Set[S] =>
  //             go(x.s, f(()))

  //           case FreeState.FlatMap(faInner, ff) =>
  //             go(s, faInner.flatMap(x => ff(x).flatMap(f)))
  //         }

  //       case FreeState.Get() =>
  //         (s, s.asInstanceOf[A])

  //       case FreeState.Set(s) =>
  //         (s, ().asInstanceOf[A])
  //     }

  //   go(s0, fa0)
  // }

  def runIdiomatic[S, A](s: S)(fa: FreeState[S, A]): (S, A) =
    fa match {
      case FreeState.Pure(a) =>
        (s, a)

      case flatmap: FreeState.FlatMap[S, x, A] =>
        val (s2, x) = runIdiomatic(s)(flatmap.fa)

        runIdiomatic(s2)(flatmap.f(x))

      case FreeState.Get() =>
        (s, s.asInstanceOf[A])

      case FreeState.Set(s) =>
        (s, ().asInstanceOf[A])
    }
}
