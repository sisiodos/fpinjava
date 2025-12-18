package org.example.app.fp.effect;

import java.util.function.Function;
import java.util.function.Supplier;

import org.example.app.fp.core.Try;
import org.example.app.fp.core.Unit;

import java.util.function.Consumer;

/**
 * {@code Io<A>} is a small, FP-friendly wrapper that defers side effects until
 * {@link #run()} is called.
 * <p>
 * Think of it as a suspended computation: instead of performing effects
 * immediately,
 * an {@code Io} stores a {@link java.util.function.Supplier} and executes it
 * only on demand.
 * This lets you compose effectful code with {@link #map(Function)} and
 * {@link #flatMap(Function)}
 * before performing any real-world actions.
 *
 * <p>
 * <strong>Core idea:</strong> Build programs as values, then run them at the
 * edge.
 * <ul>
 * <li>{@link #of(Supplier)} / {@link #pure(Object)} — create an {@code Io}</li>
 * <li>{@link #map(Function)} / {@link #flatMap(Function)} — compose without
 * executing</li>
 * <li>{@link #run()} — finally execute the deferred effect</li>
 * <li>{@link #attempt()} / {@link #runTry()} — integrate with {@code Try} for
 * safe exception capture</li>
 * <li>{@link #bracket(Function, Function)} / {@link #using(Io, Function)} —
 * structured resource handling</li>
 * </ul>
 *
 * <p>
 * Instances are immutable and thread-safe provided their contained effects are.
 *
 * @param <A> the result type produced when this {@code Io} is run
 */
public final class Io<A> {
  // ---------- Core (construction & execution) ----------

  private final Supplier<A> effect;

  private Io(Supplier<A> effect) {
    this.effect = effect;
  }

  /**
   * Lifts a side-effecting computation into {@code Io}, deferring its execution.
   */
  public static <A> Io<A> of(Supplier<A> effect) {
    return new Io<>(effect);
  }

  /**
   * Lifts a pure value into {@code Io} without effects.
   */
  public static <A> Io<A> pure(A value) {
    return new Io<>(() -> value);
  }

  /**
   * Alias of {@link #of(Supplier)} to emphasize suspension.
   */
  public static <A> Io<A> delay(Supplier<A> effect) {
    return of(effect);
  }

  /**
   * Executes the deferred side-effect and returns its result.
   */
  public A run() {
    return effect.get();
  }

  // ---------- Combinators ----------

  /**
   * Transforms the result of this {@code Io} without running it yet.
   * The effect executes only when the returned {@code Io} is {@link #run()}.
   */
  public <B> Io<B> map(Function<? super A, ? extends B> f) {
    return new Io<>(() -> f.apply(effect.get()));
  }

  /**
   * Monadic bind: sequences two {@code Io} computations by feeding this result
   * into {@code f}.
   * Neither computation executes until the returned {@code Io} is {@link #run()}.
   */
  public <B> Io<B> flatMap(Function<? super A, ? extends Io<B>> f) {
    return new Io<>(() -> f.apply(effect.get()).run());
  }

  // ---------- Safety & diagnostics ----------

  /**
   * Defers execution and, when run, captures any thrown exception into a
   * {@code Try}.
   * The returned {@code Io} itself is still lazy; calling {@link #run()} on it
   * will produce a {@code Try<A>}.
   *
   * <pre>{@code
   * Io<Try<Integer>> io = Io.of(() -> 10 / 0).attempt();
   * Try<Integer> t = io.run(); // Failure(ArithmeticException)
   * }</pre>
   */
  public Io<Try<A>> attempt() {
    return new Io<>(() -> Try.of(effect::get));
  }

  /**
   * Executes this {@code Io} immediately and returns its result wrapped in a
   * {@code Try}.
   * This is eager and intended for boundary layers.
   */
  public Try<A> runTry() {
    return Try.of(effect::get);
  }

  /**
   * Runs a side-effect on the produced value (e.g., logging/metrics) and returns
   * this value unchanged.
   * The side-effect executes only when the resulting {@code Io} is run.
   */
  public Io<A> peek(Consumer<? super A> c) {
    return new Io<>(() -> {
      A a = effect.get();
      c.accept(a);
      return a;
    });
  }

  /**
   * Sequencing operator: runs this {@code Io} and then the given {@code next},
   * ignoring this result.
   * Equivalent to {@code this.flatMap(a -> next)}.
   */
  public <B> Io<B> then(Io<B> next) {
    return flatMap(a -> next);
  }

  // ---------- Resource-safety ----------

  /**
   * Structured resource handling. Acquires a resource, uses it, and always
   * releases it, even on failure.
   * Mirrors the classic acquire–use–release pattern.
   */
  public <B> Io<B> bracket(
      Function<A, Io<B>> use,
      Function<A, Io<Unit>> release) {
    return new Io<>(() -> {
      A resource = run();
      try {
        return use.apply(resource).run();
      } finally {
        release.apply(resource).run();
      }
    });
  }

  /**
   * Variant of {@link #bracket(Function, Function)} using a {@link Consumer} for
   * release.
   * Useful when the release action returns no meaningful value.
   * <p>
   * This variant is closer to the “fire-and-forget” style of resource cleanup
   * found in Java’s {@code finally} block, while still retaining {@code Io}’s
   * composability.
   * </p>
   */
  public <B> Io<B> bracketWithRelease(
      Function<A, Io<B>> use,
      Consumer<A> release) {
    return new Io<>(() -> {
      A resource = run();
      try {
        return use.apply(resource).run();
      } finally {
        release.accept(resource);
      }
    });
  }

  // ---------- Interop (try-with-resources) ----------

  /**
   * Integrates with {@code AutoCloseable} via try-with-resources semantics.
   * The resource is acquired (lazily) from {@code acquire.run()}, passed to
   * {@code use}, and closed automatically.
   */
  public static <R extends AutoCloseable, B> Io<B> using(
      Io<R> acquire,
      Function<R, Io<B>> use) {
    return new Io<>(() -> {
      try (R res = acquire.run()) {
        return use.apply(res).run();
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
  }
}
