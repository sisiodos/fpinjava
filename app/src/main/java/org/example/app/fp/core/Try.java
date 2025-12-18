package org.example.app.fp.core;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A domain-specific {@code Try<A>} that represents a computation which may
 * either
 * result in a value ({@code Success}) or fail with a {@link Throwable}
 * ({@code Failure}).
 * <p>
 * Conceptually, {@code Try<A>} corresponds to {@code Either<Throwable, A>} with
 * a
 * right-biased API: {@link #map(Function)} and {@link #flatMap(Function)}
 * operate on the
 * success case and leave failures unchanged.
 *
 * <p>
 * <strong>Core:</strong> {@link #fold(Function, Function)} is the catamorphism
 * that
 * deconstructs {@code Success}/{@code Failure} and folds them into a single
 * value.
 *
 * <p>
 * <strong>Typical usage:</strong>
 * 
 * <pre>{@code
 * Try<Integer> t1 = Try.of(() -> 10 / 2); // Success(5)
 * Try<Integer> t2 = Try.of(() -> 10 / 0); // Failure(ArithmeticException)
 *
 * int a = t1.map(x -> x * 2).getOrElse(0); // 10
 * int b = t2.recover(ex -> 0).getOrElseThrow(RuntimeException::new); // 0, no throw
 * }</pre>
 *
 * @param <A> the success value type
 */
public sealed interface Try<A> permits Try.Success, Try.Failure {

  // ---------- Core (fold) ----------

  /**
   * Deconstructs this {@code Try}: applies {@code onFailure} to the cause if
   * failed,
   * or {@code onSuccess} to the value if succeeded, returning a single result.
   *
   * @param onFailure function to handle the failure cause
   * @param onSuccess function to handle the success value
   * @param <T>       result type
   * @return folded result
   */
  <T> T fold(
      Function<? super Throwable, ? extends T> onFailure,
      Function<? super A, ? extends T> onSuccess);

  // ---------- Constructors ----------

  /**
   * Executes the supplier, capturing thrown {@link Throwable}s as
   * {@code Failure}s.
   * This is the canonical entry point that embeds {@code try/catch} into a value.
   */
  static <A> Try<A> of(CheckedSupplier<? extends A> supplier) {
    try {
      return success(supplier.get());
    } catch (Throwable t) {
      return failure(t);
    }
  }

  /** Creates a successful {@code Try}. */
  static <A> Try<A> success(A value) {
    return new Success<>(value);
  }

  /** Creates a failed {@code Try}. */
  static <A> Try<A> failure(Throwable t) {
    return new Failure<>(t);
  }

  // ---------- Combinators ----------

  /**
   * Transforms the success value; if this is a failure, returns the same failure.
   * If the mapping function throws, the result is a {@code Failure} with the
   * thrown cause.
   */
  default <B> Try<B> map(Function<? super A, ? extends B> f) {
    // Java17 equivalent:
    // return fold(
    //   Try::failure,
    //   a -> {
    //     try { return success(f.apply(a)); }
    //     catch (Throwable t) { return failure(t); }
    //   }
    // );
    return switch (this) {
      case Failure(var t) -> failure(t);
      case Success(var a) -> {
        try { yield success(f.apply(a)); }
        catch (Throwable t2) { yield failure(t2); }
      }
    };
  }

  /**
   * Monadic bind: applies {@code f} that itself returns a {@code Try}, flattening
   * the result.
   * If this is a failure or {@code f} throws, the failure is propagated/created.
   */
  default <B> Try<B> flatMap(Function<? super A, ? extends Try<B>> f) {
    // Java17 equivalent:
    // return fold(
    //   Try::failure,
    //   a -> {
    //     try { return f.apply(a); }
    //     catch (Throwable t) { return failure(t); }
    //   }
    // );
    return switch (this) {
      case Failure(var t) -> failure(t);
      case Success(var a) -> {
        try { yield f.apply(a); }
        catch (Throwable t2) { yield failure(t2); }
      }
    };
  }

  /**
   * Runs a side-effect on the success value (e.g., logging/metrics) and returns
   * this unmodified.
   * The consumer is not invoked for failures.
   */
  default Try<A> peek(Consumer<? super A> c) {
    // Java17 equivalent:
    // return fold(
    //   Try::failure,
    //   a -> {
    //     c.accept(a);
    //     return success(a);
    //   }
    // );
    return switch (this) {
      case Failure(var t) -> failure(t);
      case Success(var a) -> {
        c.accept(a);
        yield success(a);
      }
    };
  }

  /**
   * Runs a side-effect on the failure cause (e.g., logging/metrics) and returns
   * this unmodified.
   * The consumer is not invoked for successes.
   */
  default Try<A> peekFailure(Consumer<? super Throwable> c) {
    // Java17 equivalent:
    // return fold(
    //   t -> {
    //     c.accept(t);
    //     return failure(t);
    //   },
    //   Try::success
    // );
    return switch (this) {
      case Failure(var t) -> {
        c.accept(t);
        yield failure(t);
      }
      case Success(var a) -> success(a);
    };
  }

  /**
   * Recovers from a failure by mapping the {@link Throwable} to a success value.
   * If this is a success, returns itself unchanged.
   */
  default Try<A> recover(Function<? super Throwable, ? extends A> f) {
    // Java17 equivalent:
    // return fold(
    //   t -> {
    //     try { return success(f.apply(t)); }
    //     catch (Throwable t2) { return failure(t2);} },
    //   Try::success
    // );
    return switch (this) {
      case Failure(var t) -> {
        try { yield success(f.apply(t)); }
        catch (Throwable t2) { yield failure(t2); }
      }
      case Success(var a) -> success(a);
    };
  }

  /**
   * Recovers from a failure by mapping the {@link Throwable} to another
   * {@code Try}.
   * If this is a success, returns itself unchanged.
   */
  default Try<A> recoverWith(Function<? super Throwable, ? extends Try<A>> f) {
    // Java17 equivalent:
    // return fold(
    //   t -> {
    //     try { return f.apply(t); }
    //     catch (Throwable t2) { return failure(t2);} },
    //   Try::success
    // );
    return switch (this) {
      case Failure(var t) -> {
        try { yield f.apply(t); }
        catch (Throwable t2) { yield failure(t2); }
      }
      case Success(var a) -> success(a);
    };
  }

  // ---------- Queries ----------

  /** @return {@code true} if this is a {@code Success}, else {@code false}. */
  default boolean isSuccess() {
    // Java17 equivalent:
    // return fold(
    //   t -> false,
    //   a -> true
    // );
    return switch (this) {
      case Failure(var t) -> false;
      case Success(var a) -> true;
    };
  }

  /** @return {@code true} if this is a {@code Failure}, else {@code false}. */
  default boolean isFailure() {
    // Java17 equivalent:
    // return !isSuccess();
    return switch (this) {
      case Failure(var t) -> true;
      case Success(var a) -> false;
    };
  }

  // ---------- Extractors ----------

  /** Returns the success value or the given fallback (eager). */
  default A getOrElse(A fallback) {
    // Java17 equivalent:
    // return fold(
    //   t -> fallback,
    //   a -> a
    // );
    return switch (this) {
      case Failure(var t) -> fallback;
      case Success(var a) -> a;
    };
  }

  /**
   * Returns the success value or invokes the supplier to obtain a fallback
   * (lazy).
   */
  default A getOrElseGet(Supplier<? extends A> s) {
    // Java17 equivalent:
    // return fold(
    //   t -> s.get(),
    //   a -> a
    // );
    return switch (this) {
      case Failure(var t) -> s.get();
      case Success(var a) -> a;
    };
  }

  /**
   * Returns the success value or throws a {@link RuntimeException} constructed
   * from the failure.
   * Intended for boundary layers where exceptions are expected.
   */
  default A getOrElseThrow(Function<? super Throwable, ? extends RuntimeException> toEx) {
    // Java21 equivalent (not used to preserve fold educational value):
    // return switch (this) {
    //   case Failure(var t) -> throw toEx.apply(t);
    //   case Success(var a) -> a;
    // };
    return fold(t -> {
      throw toEx.apply(t);
    }, a -> a);
  }

  // ---------- Interop ----------

  /**
   * Converts to {@link Either}: {@code Failure(t)} → {@code Left(t)},
   * {@code Success(a)} → {@code Right(a)}.
   */
  default Either<Throwable, A> toEither() {
    // Java17 equivalent:
    // return fold(
    //   Either::left,
    //   Either::right
    // );
    return switch (this) {
      case Failure(var t) -> Either.left(t);
      case Success(var a) -> Either.right(a);
    };
  }

  /**
   * Converts to {@link Option}: {@code Success(a)} → {@code Some(a)},
   * {@code Failure} → {@code None}.
   */
  default Option<A> toOption() {
    // Java17 equivalent:
    // return fold(
    //   t -> Option.none(),
    //   Option::some
    // );
    return switch (this) {
      case Failure(var t) -> Option.none();
      case Success(var a) -> Option.some(a);
    };
  }

  // ---------- Subtypes (closed by 'sealed') ----------

  /** Successful branch. */
  record Success<A>(A value) implements Try<A> {
    @Override
    public <T> T fold(Function<? super Throwable, ? extends T> onFailure,
        Function<? super A, ? extends T> onSuccess) {
      return onSuccess.apply(value);
    }

    public A get() {
      return value;
    }

    @Override
    public String toString() {
      return "Success(" + value + ")";
    }
  }

  /** Failure branch. */
  record Failure<A>(Throwable cause) implements Try<A> {
    @Override
    public <T> T fold(Function<? super Throwable, ? extends T> onFailure,
        Function<? super A, ? extends T> onSuccess) {
      return onFailure.apply(cause);
    }

    public Throwable getCause() {
      return cause;
    }

    @Override
    public String toString() {
      return "Failure(" + cause + ")";
    }
  }

  // ---------- Functional interfaces ----------

  /** A supplier whose {@link #get()} may throw a checked exception. */
  @FunctionalInterface
  interface CheckedSupplier<A> {
    A get() throws Exception;
  }
}