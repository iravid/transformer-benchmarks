package transformers

case class State[S, A](run: S => (S, A)) {
  def map[B](f: A => B): State[S, B] = State { s =>
    val res = run(s)

    (res._1, f(res._2))
  }

  def flatMap[B](f: A => State[S, B]): State[S, B] = State { s =>
    val firstRes = run(s)
    val nextAction = f(firstRes._2)

    nextAction.run(firstRes._1)
  }
}

object State {
  def pure[S, A](a: A): State[S, A] = State(s => (s, a))

  def get[S]: State[S, S] = State(s => (s, s))

  def set[S](s: S): State[S, Unit] = State(_ => (s, ()))

  def traverse[S, A, B](l: List[A])(f: A => State[S, B]): State[S, List[B]] =
    l.foldLeft(pure[S, List[B]](Nil: List[B])) { (acc, el) =>
      acc flatMap { bs =>
        f(el) map { b =>
          b :: bs
        }
      }
    } map (_.reverse)
}
