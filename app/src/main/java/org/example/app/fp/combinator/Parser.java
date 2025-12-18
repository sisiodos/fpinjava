package org.example.app.fp.combinator;

import org.example.app.fp.core.Either;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Generic parser:
 * E = environment (Reader-like), e.g. whole input, config, resources...
 * S = state (State-like), e.g. cursor/index/offset/lexer-state...
 * A = parsed value
 *
 * parse : (E, S) -> Either<Error<S>, Result<A,S>>
 * Widening choice: allows {@code other} to produce a subtype of {@code A}
 * without casts.
 * Example: {@code Parser<..., Number> p = intP.or(doubleP);}
 */
@FunctionalInterface
public interface Parser<E, S, A> {
  Either<Error<S>, Result<A, S>> parse(E env, S state);

  /**
   * Create a parser that always fails at the current state with a structured
   * {@link Error}.
   * <p>
   * This encourages <b>error injection</b>: the message is computed from the
   * running state
   * so primitives can capture positions/contexts of their own state type
   * {@code S}.
   *
   * @param message  builds the error message from the current state
   * @param expected expected token/name for diagnostics
   */
  static <E, S, A> Parser<E, S, A> fail(Function<? super S, String> message, String expected) {
    return (env, st) -> Either.left(new Error<>(message.apply(st), st, expected, false));
  }

  /**
   * Like {@link #fail(Function, String)} but marks the error as fatal (no
   * backtracking).
   */
  static <E, S, A> Parser<E, S, A> fatal(Function<? super S, String> message, String expected) {
    return (env, st) -> Either.left(new Error<>(message.apply(st), st, expected, true));
  }

  /**
   * Convenience for expected-name driven failures.
   * Example: {@code Parser.expected("digit", Object::toString)}.
   */
  static <E, S, A> Parser<E, S, A> expected(String expected, Function<? super S, String> where) {
    return fail(s -> "expected " + expected + " at " + where.apply(s), expected);
  }

  // Functor
  default <B> Parser<E, S, B> map(Function<? super A, ? extends B> f) {
    return (env, st) -> parse(env, st).map(r -> r.map(f));
  }

  // Monad
  default <B> Parser<E, S, B> flatMap(Function<? super A, Parser<E, S, B>> f) {
    return (env, st) -> parse(env, st).flatMap(r -> f.apply(r.value()).parse(env, r.next()));
    // Java21 conceptual pattern-matching equivalent:
    // return switch (parse(env, st)) {
    // case Result(var value, var next) -> f.apply(value).parse(env, next);
    // case Error(var msg, var pos, var exp, var fatal) -> Either.left(new
    // Error(...));
    // };
  }

  /**
   * Map the structured error; useful to inject labels, positions, or escalate
   * fatality.
   */
  default Parser<E, S, A> mapError(Function<? super Error<S>, ? extends Error<S>> f) {
    return (env, st) -> parse(env, st).mapLeft(f);
  }

  /**
   * Promote all failures of this parser to <b>fatal</b> failures.
   * <p>
   * This is the dual of backtracking: when wrapped in {@code commit()},
   * any error produced by this parser will prevent alternative branches
   * (e.g. {@link #or(Parser)}) from being tried.
   * <p>
   * In Parsec/Megaparsec terminology, this models a committed parse:
   * once the parser has consumed input (or the caller decides that
   * backtracking should not be allowed), subsequent failures should
   * not revert to earlier alternatives.
   *
   * <pre>
   *   p.commit().or(q)
   *   // If p fails (at any point), q is NOT tried.
   * </pre>
   *
   * This is useful for disambiguating grammars, enforcing structure,
   * or ensuring that certain branches are not silently skipped.
   */
  default Parser<E,S,A> commit() {
    return (env, st) -> parse(env, st).mapLeft(Error::asFatal);
  }

  // Alternative (choice): full backtracking at the same state
  // Contract: on failure, a parser must not advance the state.
  // If you need commit/try semantics, add dedicated combinators (e.g.,
  // attempt/commit).
  /**
   * Try this parser; if it fails (without consuming), try {@code other} from the
   * same state.
   * Left-biased: if this succeeds, {@code other} is not evaluated.
   */
  default Parser<E, S, A> or(Parser<E, S, A> other) {
    return (env, st) -> {
      var r1 = this.parse(env, st);
      if (r1.isRight())
        return r1;
      return r1.fold(
          err -> err.fatal() ? r1 : other.parse(env, st), ok -> r1);
    };
  }

  /**
   * Lazy variant: {@code other} is built only if needed.
   */
  default Parser<E, S, A> orLazy(Supplier<Parser<E, S, A>> other) {
    return (env, st) -> {
      var r1 = this.parse(env, st);
      return r1.isRight() ? r1 : other.get().parse(env, st);
    };
  }

  // Error labeling
  default Parser<E, S, A> label(Function<? super S, String> where, String name) {
    return (env, st) -> parse(env, st).mapLeft(err -> new Error<>(
        "expected " + name + " at " + where.apply(err.state()) + " but " + err.message(),
        err.state(),
        name,
        err.fatal()));
  }

  /** Successful parse: value + next state */
  record Result<A, S>(A value, S next) {
    public <B> Result<B, S> map(Function<? super A, ? extends B> f) {
      return new Result<>(f.apply(value), next);
    }
  }

  /**
   * Structured parse error carrying the state {@code S} at which the failure
   * occurred.
   */
  record Error<S>(String message, S state, String expected, boolean fatal) {
    public String pretty(Function<? super S, String> show) {
      return "Error@" + show.apply(state) + ": " + message + (expected == null ? "" : " (expected " + expected + ")")
          + (fatal ? " [fatal]" : "");
    }

    public Error<S> withExpected(String exp) {
      return new Error<>(message, state, exp, fatal);
    }

    public Error<S> asFatal() {
      return new Error<>(message, state, expected, true);
    }

    /** Alias accessor for usability in demos/logs. */
    public S pos() {
      return state;
    }
  }
}