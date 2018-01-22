package transformers

sealed abstract class RWS[E, S, L, A] { self =>
  def tag: Int

  final def map[B](f: A => B): RWS[E, S, L, B] = RWS.FlatMap(self, f.andThen(RWS.Pure(_)))

  final def flatMap[B](f: A => RWS[E, S, L, B]): RWS[E, S, L, B] =
    RWS.FlatMap(self, f)
}

object RWS {
  final def pure[E, S, L, A](a: A): RWS[E, S, L, A] = Pure(a)

  final def get[E, S, L]: RWS[E, S, L, S] = Get()

  final def set[E, S, L](s: S): RWS[E, S, L, Unit] = Set(s)

  final def ask[E, S, L]: RWS[E, S, L, E] = Ask()

  final def tell[E, S, L](l: L): RWS[E, S, L, Unit] = Tell(l)

  final def logged[E, S, L, A](l: L, a: A): RWS[E, S, L, A] = Logged(l, a)

  final def apply[E, S, L, A](f: (E, S) => (S, L, A)): RWS[E, S, L, A] = Wrap(f)

  def traverse[E, S, L, A, B](l: List[A])(f: A => RWS[E, S, L, B]): RWS[E, S, L, List[B]] =
    l.foldLeft(pure[E, S, L, List[B]](Nil: List[B])) { (acc, el) =>
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
    final val Ask = 5
    final val Tell = 6
    final val Logged = 7
    final val Wrap = 8
  }

  final case class Pure[E, S, L, A](a: A) extends RWS[E, S, L, A] {
    override final def tag = Tags.Pure
  }

  final case class FlatMap[E, S, L, A, B](fa: RWS[E, S, L, A], f: A => RWS[E, S, L, B]) 
      extends RWS[E, S, L, B] {
    override final def tag = Tags.FlatMap
  }

  final case class Get[E, S, L]() extends RWS[E, S, L, S] {
    override final def tag = Tags.Get
  }

  final case class Set[E, S, L](s: S) extends RWS[E, S, L, Unit] {
    override final def tag = Tags.Set
  }

  final case class Ask[E, S, L]() extends RWS[E, S, L, E] {
    override final def tag = Tags.Ask
  }

  final case class Tell[E, S, L](l: L) extends RWS[E, S, L, Unit] {
    override final def tag = Tags.Tell
  }

  final case class Logged[E, S, L, A](l: L, a: A) extends RWS[E, S, L, A] {
    override final def tag = Tags.Logged
  }

  final case class Wrap[E, S, L, A](f: (E, S) => (S, L, A)) extends RWS[E, S, L, A] {
    override final def tag = Tags.Wrap
  }
}

object Interpreter {
  def runOptimized[E, S, L, A](env: E, init: S, initLog: L)(fa: RWS[E, S, L, A])(combine: (L, L) => L): (S, L, A) = {
    var currOp: RWS[E, S, L, Any] = fa.asInstanceOf[RWS[E, S, L, Any]]
    var done: Boolean = false

    var conts: java.util.ArrayDeque[Any => RWS[E, S, L, Any]] = new java.util.ArrayDeque

    var log: L = initLog

    var state: S = init
    var res: Any = null

    // Put () on the stack to avoid field access
    val unit = ()

    do {
      currOp.tag match {
        case RWS.Tags.Pure =>
          res = currOp.asInstanceOf[RWS.Pure[E, S, L, A]].a

          if (conts.isEmpty())
            done = true
          else
            currOp = conts.pollFirst()(res)

        case RWS.Tags.Get =>
          res = state

          if (conts.isEmpty())
            done = true
          else
            currOp = conts.pollFirst()(res)

        case RWS.Tags.Set =>
          val op = currOp.asInstanceOf[RWS.Set[E, S, L]]

          state = op.s
          res = unit

          if (conts.isEmpty())
            done = true
          else
            currOp = conts.pollFirst()(res)

        case RWS.Tags.Ask =>
          res = env

          if (conts.isEmpty())
            done = true
          else
            currOp = conts.pollFirst()(res)

        case RWS.Tags.Tell =>
          val op = currOp.asInstanceOf[RWS.Tell[E, S, L]]

          log = combine(log, op.l)

          if (conts.isEmpty())
            done = true
          else
            currOp = conts.pollFirst()(res)

        case RWS.Tags.Logged =>
          val op = currOp.asInstanceOf[RWS.Logged[E, S, L, A]]

          res = op.a
          log = combine(log, op.l)

          if (conts.isEmpty())
            done = true
          else
            currOp = conts.pollFirst()(res)

        case RWS.Tags.Wrap =>
          val op = currOp.asInstanceOf[RWS.Wrap[E, S, L, A]]

          val wrapResult = op.f(env, state)

          state = wrapResult._1
          log = combine(log, wrapResult._2)
          res = wrapResult._3

          if (conts.isEmpty())
            done = true
          else
            currOp = conts.pollFirst()(res)

        case RWS.Tags.FlatMap =>
          val op = currOp.asInstanceOf[RWS.FlatMap[E, S, L, Any, Any]]

          op.fa.tag match {
            case RWS.Tags.Pure =>
              val nested = op.fa.asInstanceOf[RWS.Pure[E, S, L, Any]]

              res = nested.a
              currOp = op.f(nested.a)

            case RWS.Tags.Get =>
              res = state
              currOp = op.f(state)

            case RWS.Tags.Set =>
              val nested = op.fa.asInstanceOf[RWS.Set[E, S, L]]

              state = nested.s
              res = unit
              currOp = op.f(unit)

            case RWS.Tags.Ask =>
              val nested = op.fa.asInstanceOf[RWS.Ask[E, S, L]]

              res = env
              currOp = op.f(env)

            case RWS.Tags.Tell =>
              val nested = op.fa.asInstanceOf[RWS.Tell[E, S, L]]

              res = unit
              log = combine(log, nested.l)
              currOp = op.f(unit)

            case RWS.Tags.Logged =>
              val nested = currOp.asInstanceOf[RWS.Logged[E, S, L, A]]

              res = nested.a
              log = combine(log, nested.l)

              currOp = op.f(res)

            case RWS.Tags.Wrap =>
              val nested = currOp.asInstanceOf[RWS.Wrap[E, S, L, A]]

              val wrapResult = op.f(env, state)

              state = wrapResult._1
              log = combine(log, wrapResult._2)
              res = wrapResult._3

              currOp = op.f(res)

            case _ =>
              currOp = op.fa
              conts.addFirst(op.f)
          }
      }
    } while (!done)

    (state, log, res.asInstanceOf[A])
  }

  // GADT shenanigans. This should be at some point the stack safe version.
  // def runIdiomatic[S, A](s0: S)(fa0: RWS[S, A]): (S, A) = {
  //   @annotation.tailrec
  //   def go(s: S, fa: RWS[S, A]): (S, A) =
  //     fa match {
  //       case RWS.Pure(a) =>
  //         (s, a)

  //       case RWS.FlatMap(fa, f) =>
  //         fa match {
  //           case RWS.Pure(a) =>
  //             go(s, f(a))

  //           case RWS.Get() =>
  //             go(s, f(s))

  //           case x: RWS.Set[S] =>
  //             go(x.s, f(()))

  //           case RWS.FlatMap(faInner, ff) =>
  //             go(s, faInner.flatMap(x => ff(x).flatMap(f)))
  //         }

  //       case RWS.Get() =>
  //         (s, s.asInstanceOf[A])

  //       case RWS.Set(s) =>
  //         (s, ().asInstanceOf[A])
  //     }

  //   go(s0, fa0)
  // }

  def runIdiomatic[E, S, L, A](e: E, s: S, l: L)(fa: RWS[E, S, L, A])(combine: (L, L) => L): (S, L, A) =
    fa match {
      case RWS.Pure(a) =>
        (s, l, a)

      case flatmap: RWS.FlatMap[E, S, L, x, A] =>
        val (s2, l2, x) = runIdiomatic(e, s, l)(flatmap.fa)(combine)

        runIdiomatic(e, s2, l2)(flatmap.f(x))(combine)

      case RWS.Get() =>
        (s, l, s.asInstanceOf[A])

      case RWS.Set(s) =>
        (s, l, ().asInstanceOf[A])

      case RWS.Ask() =>
        (s, l, e.asInstanceOf[A])

      case RWS.Tell(l2) =>
        (s, combine(l, l2), ().asInstanceOf[A])

      case RWS.Logged(l2, a) =>
        (s, combine(l, l2), a)

      case RWS.Wrap(f) =>
        val res = f(e, s)

        (res._1, combine(l, res._2), res._3)
    }
}
