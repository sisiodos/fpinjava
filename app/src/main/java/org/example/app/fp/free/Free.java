package org.example.app.fp.free;

import java.util.function.Function;

/**
 * A generic Free monad.
 *
 * <p>
 * Free encodes a "program" as a pure data structure that is built from:
 * <ul>
 * <li>{@code Pure} : a finished computation that already has a value</li>
 * <li>{@code Suspend} : a single "instruction" in some functor {@code F}</li>
 * <li>{@code FlatMap} : sequencing of computations</li>
 * </ul>
 *
 * The important point is that {@code Free} itself does not know how to
 * interpret
 * the instructions in {@code F}. That responsibility is delegated to an
 * interpreter which will later "run" the program described by the Free value.
 *
 * @param <F> Instruction functor (a type constructor such as Expr<A>, IoF<A>,
 *            etc.)
 * @param <A> Result type of the program
 */
public sealed interface Free<F, A>
    permits Free.Pure, Free.Suspend, Free.FlatMapped {

  /**
   * Represents a computation that has already finished with value {@code value}.
   */
  record Pure<F, A>(A value) implements Free<F, A> {
  }

  /**
   * Represents a single suspended instruction in the base functor {@code F}.
   *
   * <p>
   * Typically {@code F} will be an algebra / instruction set such as:
   * 
   * <pre>
   *   sealed interface ExprF<A> permits Get, Put, ...
   * </pre>
   * 
   * and {@code Suspend<ExprF, A>} will hold one node of that instruction.
   */
  record Suspend<F, A>(F fa) implements Free<F, A> {
  }

  /**
   * Represents the monadic bind (flatMap) structure.
   *
   * <p>
   * This node says: first run {@code sub}, then feed its result into {@code k}
   * to obtain the remainder of the program.
   */
  record FlatMapped<F, A, B>(
      Free<F, A> sub,
      Function<? super A, Free<F, B>> k) implements Free<F, B> {
  }

  // -------------------------------------------------------------------------
  // Constructors
  // -------------------------------------------------------------------------

  /**
   * Lifts a plain value into a finished Free computation.
   */
  static <F, A> Free<F, A> pure(A value) {
    return new Pure<>(value);
  }

  /**
   * Lifts a single instruction in {@code F} into a Free program.
   *
   * <p>
   * If you have an instruction type such as {@code ExprF<A>},
   * {@code liftF(expr)} turns it into a one-step Free program.
   */
  static <F, A> Free<F, A> liftF(F fa) {
    return new Suspend<>(fa);
  }

  // -------------------------------------------------------------------------
  // Functor / Monad operations
  // -------------------------------------------------------------------------

  /**
   * Functor map: transform the result value while keeping the program structure.
   */
  default <B> Free<F, B> map(Function<? super A, ? extends B> f) {
    return flatMap(a -> pure(f.apply(a)));
  }

  /**
   * Monad bind (flatMap): sequence this program with the next one.
   *
   * <p>
   * This does not "run" the program. It only builds a larger program
   * as a pure data structure.
   */
  default <B> Free<F, B> flatMap(Function<? super A, Free<F, B>> f) {
    return new FlatMapped<>(this, f);
  }

  // -------------------------------------------------------------------------
  // Program stepping (small-step interpreter core)
  // -------------------------------------------------------------------------

  /**
   * Performs a single small-step evaluation:
   * rewrites nested FlatMapped nodes into a right-associated form.
   *
   * <p>
   * This is a standard helper used by interpreters to avoid deep recursion
   * when running Free programs.
   */
  default Free<F, A> step() {
    Free<F, A> curr = this;

    while (true) {
      if (curr instanceof FlatMapped<F, ?, ?> fm) {
        Free<F, ?> sub = fm.sub();
        Function<Object, Free<F, A>> k = unsafeCast(fm.k());

        if (sub instanceof FlatMapped<F, ?, ?> subFm) {
          // reassociate: (m >>= f) >>= g === m >>= (x -> f(x) >>= g)
          Free<F, ?> m = subFm.sub();
          Function<Object, Free<F, ?>> f = unsafeCast(subFm.k());

          Function<Object, Free<F, A>> g = x -> {
            Free<F, Object> mid = unsafeCast(f.apply(x));
            return mid.flatMap(k);
          };

          Free<F, A> reassociated = unsafeCast(new FlatMapped<>(m, g));
          curr = reassociated;
          continue;
        }
      }
      // If we cannot reassociate further, return as-is.
      return curr;
    }
  }

  @SuppressWarnings("unchecked")
  private static <T> T unsafeCast(Object o) {
    return (T) o;
  }

  // -------------------------------------------------------------------------
  // fold / interpret は、この Free と DSL ごとに別ファイルや別クラスで
  // 実装していく想定とする。
  // 例:
  //
  // interface FunctionK<F, M> {
  // <A> M apply(F fa);
  // }
  //
  // <M> M foldMap(FunctionK<F, M> interpreter, Monad<M> M) { ... }
  //
  // Java では高カインド型を直接表現できないため、
  // ここでは Free 本体は構造の定義だけに留めている。
  // -------------------------------------------------------------------------
}
