
package org.example.app.fp.effect;

import java.util.function.Function;

/**
 * A purely functional representation of a stateful computation.
 *
 * <p>{@code State&lt;S, A&gt;} encodes a computation that:
 * <ul>
 *   <li>receives an input state of type {@code S},</li>
 *   <li>produces a (possibly) new state of type {@code S},</li>
 *   <li>and yields a result value of type {@code A}.</li>
 * </ul>
 *
 * <p>Formally, it wraps a single function of the shape:
 * <pre>{@code
 *   S -> (S, A)
 * }</pre>
 * where the pair {@code (S, A)} is represented by the nested {@link Result} record.
 *
 * <h2>Design</h2>
 * <ul>
 *   <li>{@code State} keeps state-manipulation <em>pure</em> by making the
 *   state flow explicit via arguments/returns, rather than hidden mutation.</li>
 *   <li>Composition is expressed via {@link #map(Function)} and
 *   {@link #flatMap(Function)}; evaluation is explicit via {@link #run(Object)},
 *   {@link #eval(Object)}, and {@link #exec(Object)}.</li>
 *   <li>Utilities {@link #get()}, {@link #set(Object)}, {@link #modify(Function)},
 *   and {@link #inspect(Function)} model reading, writing, updating, and deriving
 *   from the state respectively.</li>
 *   <li>{@link #mapState(Function)} allows post-processing of the <em>resulting</em> state.</li>
 * </ul>
 *
 * <h2>Example</h2>
 * <pre>{@code
 * // Counter: increment twice, then read
 * State<Integer, String> prog =
 *     State.modify((Integer s) -> s + 1)
 *          .then(State.modify((Integer s) -> s + 1))
 *          .then(State.inspect((Integer s) -> "count = " + s));
 *
 * var r = prog.run(0);   // Result[state=2, value="count = 2"]
 * }</pre>
 *
 * @param <S> the type of the state threaded through the computation
 * @param <A> the type of the value produced by the computation
 */
public final class State<S, A> {

  /**
   * The underlying state transition function:
   * takes an input state and returns the new state paired with a value.
   */
  private final Function<S, Result<S, A>> run;

  /**
   * Creates a {@code State} from its underlying state transition function.
   *
   * @param run the pure function {@code S -> Result&lt;S, A&gt;} to execute
   */
  public State(Function<S, Result<S, A>> run) {
    this.run = run;
  }

  /**
   * Executes this stateful computation on the given initial state.
   *
   * <p>This is the only place where the underlying function is applied.
   * Most users will prefer {@link #eval(Object)} or {@link #exec(Object)} when
   * they only need either the value or the state.
   *
   * @param state the initial state
   * @return the {@link Result} containing the resulting state and value
   */
  public Result<S, A> run(S state) {
    return run.apply(state);
  }

  /**
   * Executes the computation and returns only the produced value.
   *
   * @param initial the initial state
   * @return the resulting value
   */
  public A eval(S initial) {
    return run(initial).value();
  }

  /**
   * Executes the computation and returns only the resulting state.
   *
   * @param initial the initial state
   * @return the resulting state
   */
  public S exec(S initial) {
    return run(initial).state();
  }

  /**
   * Functor map: transforms the produced value, leaving the state flow unchanged.
   *
   * <p>Law (functor):
   * <pre>{@code
   *   state.map(id) == state
   *   state.map(f).map(g) == state.map(f.andThen(g))
   * }</pre>
   *
   * @param <B> the new value type
   * @param f   function to transform the produced value
   * @return a new {@code State} that applies {@code f} to the value
   */
  public <B> State<S, B> map(Function<? super A, ? extends B> f) {
    return new State<>(s -> {
      Result<S, A> result = run(s);
      return new Result<>(result.state(), f.apply(result.value()));
    });
  }

  /**
   * Monad bind: sequences computations by using the current value
   * to determine the next stateful computation.
   *
   * <p>Intuition: run this state to get {@code (s1, a)}, then compute
   * a new stateful computation from {@code a}, and run it from {@code s1}.
   *
   * <p>Monad laws:
   * <pre>{@code
   * // left identity
   * pure(a).flatMap(f) == f.apply(a)
   *
   * // right identity
   * m.flatMap(State::pure) == m
   *
   * // associativity
   * m.flatMap(f).flatMap(g) == m.flatMap(a -> f.apply(a).flatMap(g))
   * }</pre>
   *
   * @param <B> the next computation's value type
   * @param f   function from current value to the next stateful computation
   * @return the composed {@code State}
   */
  public <B> State<S, B> flatMap(Function<? super A, State<S, B>> f) {
    return new State<>(s -> {
      Result<S, A> result = run(s);
      return f.apply(result.value()).run(result.state());
    });
  }

  /**
   * Sequences another stateful computation, ignoring the current value.
   *
   * <p>Equivalent to {@code flatMap(_ -> next)}. Useful when only the
   * state transition effects matter and the intermediate values are irrelevant.
   *
   * @param <B>  the next computation's value type
   * @param next the computation to run next
   * @return {@code next} executed after this computation's state transition
   */
  public <B> State<S, B> then(State<S, B> next) {
    return flatMap(ignored -> next);
  }

  /**
   * Lifts a pure value into the {@code State} context without changing the state.
   *
   * @param <S> the state type
   * @param <A> the value type
   * @param a   the value to lift
   * @return a {@code State} that returns {@code a} and preserves the input state
   */
  public static <S, A> State<S, A> pure(A a) {
    return new State<>(s -> new Result<>(s, a));
  }

  /**
   * Reads (observes) the current state and returns it as the value.
   *
   * <p>Alias for the common pattern “read without modification”.
   *
   * @param <S> the state type
   * @return a {@code State} that yields the current state as its value
   */
  public static <S> State<S, S> get() {
    return new State<>(s -> new Result<>(s, s));
  }

  /**
   * Replaces the current state with {@code newState}.
   *
   * <p>This models an imperative assignment in a pure way. The produced value
   * is {@code null} (use a custom {@code Unit} type if you prefer to avoid {@code null}).
   *
   * @param <S>      the state type
   * @param newState the new state to set
   * @return a {@code State} that overwrites the state with {@code newState}
   */
  public static <S> State<S, Void> set(S newState) {
    return new State<>(s -> new Result<>(newState, null));
  }

  /**
   * Updates the current state via a pure function.
   *
   * <p>Conceptually: {@code state = f(state)} in imperative terms.
   * The produced value is {@code null} (consider a {@code Unit} type if desired).
   *
   * @param <S> the state type
   * @param f   state transformer
   * @return a {@code State} that applies {@code f} to the current state
   */
  public static <S> State<S, Void> modify(Function<? super S, ? extends S> f) {
    return new State<>(s -> new Result<>(f.apply(s), null));
  }

  /**
   * Computes a derived value from the current state without modifying it.
   *
   * <p>Useful for pure observations/queries on the state.
   *
   * @param <S> the state type
   * @param <A> the derived value type
   * @param f   function to derive a value from the current state
   * @return a {@code State} that returns {@code f(state)} while leaving state unchanged
   */
  public static <S, A> State<S, A> inspect(Function<? super S, ? extends A> f) {
    return new State<>(s -> new Result<>(s, f.apply(s)));
  }

  /**
   * Transforms the <em>resulting</em> state (post-execution) using a pure function.
   *
   * <p>After this computation runs and produces {@code (state, value)}, the
   * state component is transformed by {@code g}. The value is left unchanged.
   * This is useful for normalization or post-processing of the state.
   *
   * @param g function to transform the output state
   * @return a {@code State} that applies {@code g} to the produced state
   */
  public State<S, A> mapState(Function<? super S, ? extends S> g) {
    return new State<>(s -> {
      Result<S, A> r = run(s);
      return new Result<>(g.apply(r.state()), r.value());
    });
  }

  /**
   * The result of executing a {@link State}: a pair of the new state and the produced value.
   *
   * <p>This record intentionally has no behavior; it is a simple product type used
   * to make the data flow explicit and readable.
   *
   * @param <S> the state type
   * @param <A> the value type
   */
  public static record Result<S, A>(S state, A value) { }
}
