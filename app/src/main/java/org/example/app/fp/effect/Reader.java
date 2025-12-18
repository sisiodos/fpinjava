package org.example.app.fp.effect;

import java.util.function.Function;

/**
 * Functional "Reader" effect: a computation that depends on an environment value R.
 *
 * <p>This is the Java 21 version implemented as a record, keeping the definition minimal.
 *
 * <p>The Reader monad captures the idea of a function R -> A.
 * It is useful when a series of computations:
 *  - require access to shared configuration,
 *  - should remain pure (no mutation, no globals),
 *  - and must thread the same environment through a chain of operations.
 *
 * <p>flatMap corresponds to Kleisli composition:
 * If f: A -> Reader<R, B> and g: B -> Reader<R, C>,
 * then flatMap ensures both functions run under the same environment R.
 *
 * <p>Key combinators:
 * <ul>
 *   <li>{@code ask()} — obtain the full environment inside the computation.</li>
 *   <li>{@code local(...)} — run a computation under a “modified” environment.</li>
 * </ul>
 *
 * <p>Java 17 equivalent (for educational contrast) is provided below in comments.
 * // public final class Reader<R, A> {
 * // private final Function<R, A> run;
 * //
 * // public Reader(Function<R, A> run) { this.run = run; }
 * //
 * // public A run(R env) { return run.apply(env); }
 * //
 * // public <B> Reader<R, B> map(Function<? super A, ? extends B> f) {
 * // return new Reader<>(r -> f.apply(run.apply(r)));
 * // }
 * //
 * // public <B> Reader<R, B> flatMap(Function<? super A, Reader<R, B>> f) {
 * // return new Reader<>(r -> f.apply(run.apply(r)).run(r));
 * // }
 * // }
 */
public record Reader<R, A>(Function<R, A> run) {

  public A run(R env) {
    return run.apply(env);
  }

  public <B> Reader<R, B> map(Function<? super A, ? extends B> f) {
    return new Reader<>(r -> f.apply(run.apply(r)));
  }

  public <B> Reader<R, B> flatMap(Function<? super A, Reader<R, B>> f) {
    return new Reader<>(r -> f.apply(run.apply(r)).run.apply(r));
  }

  /**
   * Ask for the whole environment.
   * Equivalent to Haskell's "ask".
   */
  public static <R> Reader<R, R> ask() {
    return new Reader<>(r -> r);
  }

  /**
   * Local modification of the environment.
   * Equivalent to Haskell's "local".
   */
  public Reader<R, A> local(Function<? super R, ? extends R> modify) {
    return new Reader<>(r -> run.apply(modify.apply(r)));
  }
}