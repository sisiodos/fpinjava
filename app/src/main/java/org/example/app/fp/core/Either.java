package org.example.app.fp.core;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A disjoint union of two values, {@code Left<L>} or {@code Right<R>}.
 * <p>
 * By convention, {@code Left} carries an error or exceptional value, while
 * {@code Right}
 * carries a successful/expected result. This convention enables a
 * "right-biased" API:
 * operations like {@link #map(Function)} and {@link #flatMap(Function)}
 * transform
 * the {@code Right} side (the happy path), and leave the {@code Left} side
 * unchanged.
 *
 * <p>
 * <strong>Core idea:</strong>
 * {@link #fold(Function, Function)} is the fundamental operation (a
 * catamorphism) that
 * deconstructs the two cases and <em>folds</em> them into a single result
 * value. Most
 * combinators in this class can be defined in terms of {@code fold}.
 *
 * <p>
 * <strong>Immutability:</strong>
 * Instances are immutable and thread-safe provided their contained values are.
 *
 * <p>
 * <strong>Typical usage:</strong>
 * 
 * <pre>{@code
 * // Success path (Right)
 * Either<String, Integer> ok = Either.right(21)
 *     .map(x -> x * 2) // Right(42)
 *     .flatMap(x -> x % 2 == 0
 *         ? Either.right(x)
 *         : Either.left("odd"));
 *
 * // Error path (Left)
 * Either<String, Integer> err = Either.<String, Integer>left("not a number")
 *     .map(x -> x + 1); // stays Left("not a number")
 *
 * // Consuming the result
 * int value = ok.fold(
 *     message -> -1, // onLeft
 *     r -> r // onRight
 * );
 * }</pre>
 *
 * <p>
 * <strong>See also:</strong> {@link #map(Function)},
 * {@link #flatMap(Function)},
 * {@link #mapLeft(Function)}, {@link #bimap(Function, Function)},
 * {@link #fold(Function, Function)}.
 *
 * @param <L> the type of the left value (often used for errors)
 * @param <R> the type of the right value (often used for successful results)
 */
public sealed interface Either<L, R> permits Either.Left, Either.Right {

  // ---------- Core (fold) ----------

  /**
   * Deconstructs this {@code Either} (choosing {@code Left} or {@code Right})
   * and <em>folds</em> it into a single result value of type {@code T}.
   * <p>
   * The term “fold” here is intentional: it does not merely deconstruct the
   * structure,
   * it <strong>aggregates</strong> the two possible cases into one value (reduces
   * the
   * sum type to a plain value). In category-theoretic terms, this method is the
   * catamorphism of {@code Either}.
   *
   * <p>
   * Operationally:
   * <ul>
   * <li>If this is a {@code Left(l)}, then {@code onLeft.apply(l)} is
   * returned.</li>
   * <li>If this is a {@code Right(r)}, then {@code onRight.apply(r)} is
   * returned.</li>
   * </ul>
   * Exactly one of the functions is applied.
   *
   * <p>
   * Equivalently to pattern matching:
   * 
   * <pre>{@code
   * return switch (either) {
   *   case Either.Left(L l) -> onLeft.apply(l);
   *   case Either.Right(R r) -> onRight.apply(r);
   * };
   * }</pre>
   *
   * <p>
   * <strong>Design note:</strong>
   * All other combinators such as {@code map}, {@code mapLeft}, and
   * {@code flatMap}
   * can be (and are, in this implementation) defined in terms of {@code fold}.
   * {@code fold} is the primary "exit" that turns the two-case structure into a
   * single value.
   *
   * <p>
   * Examples:
   * 
   * <pre>{@code
   * Either<String, Integer> e1 = Either.right(42);
   * Either<String, Integer> e2 = Either.left("error");
   *
   * int r1 = e1.fold(err -> -1, val -> val); // 42
   * int r2 = e2.fold(err -> -1, val -> val); // -1
   * }</pre>
   */
  <T> T fold(
      Function<? super L, ? extends T> onLeft,
      Function<? super R, ? extends T> onRight);

  // ---------- Subtypes (closed by 'sealed') ----------

  /**
   * Represents the {@code Left} case of an {@link Either}, conventionally used
   * to hold an error or exceptional value.
   */
  record Left<L, R>(L value) implements Either<L, R> {
    @Override
    public <T> T fold(Function<? super L, ? extends T> onLeft,
        Function<? super R, ? extends T> onRight) {
      return onLeft.apply(value);
    }

    public L getLeft() {
      return value;
    }
  }

  /**
   * Represents the {@code Right} case of an {@link Either}, conventionally used
   * to hold a successful or expected result value.
   */
  record Right<L, R>(R value) implements Either<L, R> {
    @Override
    public <T> T fold(Function<? super L, ? extends T> onLeft,
        Function<? super R, ? extends T> onRight) {
      return onRight.apply(value);
    }

    public R getRight() {
      return value;
    }
  }

  // ---------- Factories ----------

  /** Creates a {@code Left} instance containing the given value. */
  static <L, R> Either<L, R> left(L value) {
    return new Left<>(value);
  }

  /** Creates a {@code Right} instance containing the given value. */
  static <L, R> Either<L, R> right(R value) {
    return new Right<>(value);
  }

  // ---------- Combinators (default methods) ----------

  /** Right-biased map. */
  default <R2> Either<L, R2> map(Function<? super R, ? extends R2> f) {
    // Java17 equivalent:
    // return fold(Either::left, r -> right(f.apply(r)));
    return switch (this) {
      case Left(var l) -> left(l);
      case Right(var r) -> right(f.apply(r));
    };
  }

  /** Left map (transform error/info side). */
  default <L2> Either<L2, R> mapLeft(Function<? super L, ? extends L2> f) {
    // Java17 equivalent:
    // return fold(l -> left(f.apply(l)), Either::right);
    return switch (this) {
      case Left(var l) -> left(f.apply(l));
      case Right(var r) -> right(r);
    };
  }

  /** Monadic bind for Either. */
  default <R2> Either<L, R2> flatMap(Function<? super R, ? extends Either<L, R2>> f) {
    // Java17 equivalent:
    // return fold(Either::left, f);
    return switch (this) {
      case Left(var l) -> left(l);
      case Right(var r) -> f.apply(r);
    };
  }

  /** Bifunctor map: transform both sides at once. */
  default <L2, R2> Either<L2, R2> bimap(
      Function<? super L, ? extends L2> fl,
      Function<? super R, ? extends R2> fr) {
    // Java17 equivalent:
    // return fold(
    //   l -> left(fl.apply(l)),
    //   r -> right(fr.apply(r))
    // );
    return switch (this) {
      case Left(var l) -> left(fl.apply(l));
      case Right(var r) -> right(fr.apply(r));
    };
  }

  // ---------- Queries ----------

  /** @return {@code true} if this is a {@code Left}. */
  default boolean isLeft() {
    // Java17 equivalent:
    // return this instanceof Left<?, ?>;
    return switch (this) {
      case Left(var l) -> true;
      case Right(var r) -> false;
    };
  }

  /** @return {@code true} if this is a {@code Right}. */
  default boolean isRight() {
    // Java17 equivalent:
    // return this instanceof Right<?, ?>;
    return switch (this) {
      case Left(var l) -> false;
      case Right(var r) -> true;
    };
  }

  // ---------- Factories (lifting) ----------

  /** Lift a possibly-null reference into Either. */
  static <L, R> Either<L, R> fromNullable(R r, Supplier<? extends L> ifNull) {
    return r != null ? right(r) : left(ifNull.get());
  }

  // ---------- Transformers ----------

  /** Swap left/right. */
  default Either<R, L> swap() {
    // Java17 equivalent:
    // return fold(Either::right, Either::left);
    return switch (this) {
      case Left(var l) -> right(l);
      case Right(var r) -> left(r);
    };
  }

  // ---------- Extractors ----------

  /** Eager fallback. */
  default R getOrElse(R fallback) {
    return fold(
        l -> fallback,
        r -> r);
  }

  /** Lazy fallback. */
  default R getOrElseGet(Supplier<? extends R> s) {
    return fold(
        l -> s.get(),
        r -> r);
  }

  /** Escape hatch: throw RuntimeException derived from Left. */
  default R getOrElseThrow(Function<? super L, ? extends RuntimeException> toEx) {
    return fold(
        l -> { throw toEx.apply(l);},
        r -> r);
  }

  // ---------- Interop ----------

  /** Right to Optional. */
  default java.util.Optional<R> toOptional() {
    // Java17 equivalent:
    // return fold(
    //   l -> java.util.Optional.empty(),
    //   java.util.Optional::ofNullable
    // );

    return switch (this) {
      case Left(var l) -> java.util.Optional.empty();
      case Right(var r) -> java.util.Optional.ofNullable(r);
    };
  }

  /** Left to Optional. */
  default java.util.Optional<L> leftOptional() {
    // Java17 equivalent:
    // return fold(
    //   java.util.Optional::ofNullable,
    //   r -> java.util.Optional.empty()
    // );

    return switch (this) {
      case Left(var l) -> java.util.Optional.ofNullable(l);
      case Right(var r) -> java.util.Optional.empty();
    };
  }

  // ---------- Recovery ----------

  /** Recover Left to Right by mapping error to a value. */
  default Either<L, R> recover(Function<? super L, ? extends R> f) {
    return fold(l -> right(f.apply(l)), r -> this);
  }

  /** Recover Left by providing an alternative Either. */
  default Either<L, R> recoverWith(Function<? super L, ? extends Either<L, R>> f) {
    return fold(f, r -> this);
  }
}