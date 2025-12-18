package org.example.app.fp.effect;

import java.util.function.BinaryOperator;
import java.util.function.Function;

/**
 * A Writer monad captures a value together with an accumulated log.
 * It is the dual of Reader: whereas Reader *reads* an environment,
 * Writer *accumulates* information.
 *
 * @param <W> The log type (typically a List<String> or another monoidal structure)
 * @param <A> The result value
 */
public record Writer<W, A>(A value, W log, BinaryOperator<W> append) {

  /**
   * Creates a writer with a value and an empty log.
   */
  public static <W, A> Writer<W, A> pure(A value, W emptyLog, BinaryOperator<W> append) {
    return new Writer<>(value, emptyLog, append);
  }

  /**
   * Appends log output using the monoid-like append operator.
   */
  public Writer<W, A> tell(W newLog) {
    return new Writer<>(value, append.apply(log, newLog), append);
  }

  /**
   * Functor map: transforms the value but preserves the accumulated log.
   */
  public <B> Writer<W, B> map(Function<? super A, ? extends B> f) {
    return new Writer<>(f.apply(value), log, append);
  }

  /**
   * Monad bind (flatMap):
   * runs the next computation, obtains its log, and merges logs using append.
   */
  public <B> Writer<W, B> flatMap(Function<? super A, Writer<W, B>> f) {
    Writer<W, B> next = f.apply(value);
    W combined = append.apply(log, next.log());
    return new Writer<>(next.value(), combined, append);
  }
}
