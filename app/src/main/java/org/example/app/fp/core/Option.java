package org.example.app.fp.core;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * An immutable, FP-friendly {@code Option<A>} — a value that may be present or absent —
 * expressed as a <em>closed</em> algebraic data type using modern Java
 * ({@code sealed interface} + {@code record}/{@code enum}).
 * <p>
 * Two cases only:
 * <ul>
 *   <li>{@link Some} — holds a value of type {@code A}</li>
 *   <li>{@link None} — holds no value</li>
 * </ul>
 *
 * <h2>Design</h2>
 * <ul>
 *   <li>{@link #fold(Function, Supplier)} is the fundamental operation (a catamorphism)
 *     that deconstructs the two cases and reduces them to a single result.</li>
 *   <li>Right from this, the usual combinators ({@link #map(Function)}, {@link #flatMap(Function)},
 *     {@link #filter(Predicate)}, etc.) are defined as <em>default</em> methods on the interface.</li>
 *   <li>Instances are immutable and thread-safe provided their contents are.</li>
 * </ul>
 *
 * <h2>Example</h2>
 * <pre>{@code
 * Option<Integer> o1 = Option.some(21).map(x -> x * 2);   // Some(42)
 * Option<Integer> o2 = Option.<Integer>none().map(x -> x * 2); // None
 * int v = o1.fold(x -> x, () -> 0);                      // 42
 * }
 * </pre>
 *
 * <p>Compared to {@link java.util.Optional}, this type emphasizes fold-based
 * composition and seamless interop with {@link Either}.</p>
 */
public sealed interface Option<A> permits Option.Some, Option.None {

  // ---------------------------------------------------------------------------
  // Core (fold)
  // ---------------------------------------------------------------------------

  /**
   * Deconstructs this {@code Option}: applies {@code onSome} to the value if present,
   * otherwise evaluates {@code onNone} and returns its result.
   *
   * <p>Pattern-matching equivalent:</p>
   * <pre>{@code
   * return switch (opt) {
   *   case Option.Some(A a) -> onSome.apply(a);
   *   case Option.None      -> onNone.get();
   * };
   * }</pre>
   */
  <T> T fold(Function<? super A, ? extends T> onSome,
             Supplier<? extends T> onNone);

  // ---------------------------------------------------------------------------
  // Constructors / factories
  // ---------------------------------------------------------------------------

  /** Create {@code Some(value)} (value may be {@code null}; consider {@link #fromNullable(Object)}). */
  static <A> Option<A> some(A value) { return new Some<>(value); }

  /** Create {@code None}. */
  static <A> Option<A> none() { return new None<>(); }

  /** {@code value != null ? Some(value) : None}. */
  static <A> Option<A> fromNullable(A value) { return value != null ? some(value) : none(); }

  // ---------------------------------------------------------------------------
  // Combinators
  // ---------------------------------------------------------------------------

  /** Transform the contained value if present; otherwise {@code None}. */
  default <B> Option<B> map(Function<? super A, ? extends B> f) {
    // Java17 equivalent:
    // return fold(
    //   a -> some(f.apply(a)),
    //   Option::none
    // );
    return switch (this) {
      case Some(var a) -> some(f.apply(a));
      case None()      -> Option.none();
    };
  }

  /** Monadic bind: apply {@code f} returning an {@code Option}, and flatten. */
  default <B> Option<B> flatMap(Function<? super A, ? extends Option<B>> f) {
    // Java17 equivalent:
    // return fold(
    //   f,
    //   Option::none
    // );
    return switch (this) {
      case Some(var a) -> f.apply(a);
      case None()      -> Option.none();
    };
  }

  /** Keep the value only if {@code p} holds; otherwise {@code None}. */
  default Option<A> filter(Predicate<? super A> p) {
    // Java17 equivalent:
    // return fold(
    //   a -> p.test(a) ? this : Option.none(),
    //   Option::none
    // );
    return switch (this) {
      case Some(var a) -> p.test(a) ? this : Option.none();
      case None()      -> Option.none();
    };
  }

  /** Applicative apply: if both this and {@code ff} are Some, apply the function. */
  default <B> Option<B> ap(Option<Function<? super A, ? extends B>> ff) {
    // Java17 equivalent:
    // return ff.flatMap(f -> map(f));
    return switch (ff) {
      case Some(var func) -> switch (this) {
        case Some(var a) -> some(func.apply(a));
        case None()      -> Option.none();
      };
      case None() -> Option.none();
    };
  }

  // ---------------------------------------------------------------------------
  // Queries
  // ---------------------------------------------------------------------------

  /** @return {@code true} if this is {@code Some}, else {@code false}. */
  default boolean isDefined() {
    // Java17 equivalent:
    // return fold(
    //   a -> true,
    //   () -> false
    // );
    return switch (this) {
      case Some(var a) -> true;
      case None()      -> false;
    };
  }

  /** @return {@code true} if this is {@code None}, else {@code false}. */
  default boolean isEmpty() {
    // Java17 equivalent:
    // return !isDefined();
    return switch (this) {
      case Some(var a) -> false;
      case None()      -> true;
    };
  }

  // ---------------------------------------------------------------------------
  // Consumers / extractors
  // ---------------------------------------------------------------------------

  /**
   * Returns the contained value if present, otherwise throws {@link IllegalStateException}.
   * <p>This method is provided for interoperability and imperative use;
   * prefer using {@link #fold(Function, Supplier)} or {@link #getOrElse(Object)} in FP-style code.</p>
   */
  default A get() {
    // Java17 equivalent:
    // return fold(
    //   a -> a,
    //   () -> { throw new IllegalStateException("No value present"); }
    // );
    return switch (this) {
      case Some(var a) -> a;
      case None()      -> throw new IllegalStateException("No value present");
    };
  }

  /** Return the value if present, otherwise {@code fallback} (eager). */
  default A getOrElse(A fallback) {
    // Java17 equivalent:
    // return fold(
    //   a -> a,
    //   () -> fallback
    // );
    return switch (this) {
      case Some(var a) -> a;
      case None()      -> fallback;
    };
  }

  /** Return the value if present, otherwise {@code supplier.get()} (lazy). */
  default A getOrElseGet(Supplier<? extends A> supplier) {
    // Java17 equivalent:
    // return fold(
    //   a -> a,
    //   supplier
    // );
    return switch (this) {
      case Some(var a) -> a;
      case None()      -> supplier.get();
    };
  }

  /** Return this if defined, otherwise {@code other} (eager). */
  default Option<A> orElse(Option<A> other) {
    // Java17 equivalent:
    // return fold(
    //   a -> this,
    //   () -> other
    // );
    return switch (this) {
      case Some(var a) -> this;
      case None()      -> other;
    };
  }

  /** Return this if defined, otherwise {@code supplier.get()} (lazy). */
  default Option<A> orElseGet(Supplier<? extends Option<A>> supplier) {
    // Java17 equivalent:
    // return fold(
    //   a -> this,
    //   supplier
    // );
    return switch (this) {
      case Some(var a) -> this;
      case None()      -> supplier.get();
    };
  }

  /** Convert to {@link java.util.Optional}. */
  default java.util.Optional<A> toOptional() {
    // Java17 equivalent:
    // return fold(
    //   Optional::ofNullable,
    //   Optional::empty
    // );
    return switch (this) {
      case Some(var a) -> java.util.Optional.ofNullable(a);
      case None()      -> java.util.Optional.empty();
    };
  }

  /**
   * Convert to {@link Either}: {@code Some(a) -> Right(a)}, {@code None -> Left(ifNone.get())}.
   */
  default <L> Either<L, A> toEither(Supplier<? extends L> ifNone) {
    // Java17 equivalent:
    // return fold(
    //   Either::right,
    //   () -> Either.left(ifNone.get())
    // );
    return switch (this) {
      case Some(var a) -> Either.right(a);
      case None()      -> Either.left(ifNone.get());
    };
  }

  /** Return the raw value or {@code null}. Use with care. */
  default A orNull() {
    // Java17 equivalent:
    // return fold(
    //   a -> a,
    //   () -> null
    // );
    return switch (this) {
      case Some(var a) -> a;
      case None()      -> null;
    };
  }

  // ---------------------------------------------------------------------------
  // Cases (closed by `sealed`)
  // ---------------------------------------------------------------------------

  /** {@code Some(a)} branch. */
  record Some<A>(A value) implements Option<A> {
    @Override public <T> T fold(Function<? super A, ? extends T> onSome,
                                Supplier<? extends T> onNone) { return onSome.apply(value); }
    public A get() { return value; }
    @Override public String toString() { return "Some(" + value + ")"; }
    @Override public boolean equals(Object o) {
      return (o instanceof Some<?> s) && Objects.equals(this.value, s.value);
    }
    @Override public int hashCode() { return Objects.hashCode(value); }
  }

  // Java17-era enum implementation retained for reference:
  // enum None implements Option<Object> { INSTANCE;
  //   @SuppressWarnings("unchecked")
  //   static <A> Option<A> instance() { return (Option<A>) INSTANCE; }
  //   @Override public <T> T fold(Function<? super Object, ? extends T> onSome,
  //                               Supplier<? extends T> onNone) { return onNone.get(); }
  //   @Override public String toString() { return "None"; }
  // }

  /**
   * {@code None} branch using a record for Java 21 pattern matching compatibility.
   * This is not a physical singleton, but all instances are indistinguishable.
   */
  record None<A>() implements Option<A> {
    @Override
    public <T> T fold(Function<? super A, ? extends T> onSome,
                      Supplier<? extends T> onNone) {
      return onNone.get();
    }

    @Override
    public String toString() { return "None"; }
  }
}