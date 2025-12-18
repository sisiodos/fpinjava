package org.example.app.fp.demo.state;

import org.example.app.fp.effect.State;

/**
 * Counter implemented with the State monad.
 *
 * This implementation follows the canonical tutorial-style model used in FP literature:
 *   - The returned value (A) and the new state (S) always remain equal.
 *   - Each operation produces a new counter value, and the next state is that same value.
 *
 * In other words:
 *     S -> (A, S)
 * where A == S in every step.
 *
 * This is the most common pedagogical interpretation of a "counter" expressed as a State monad.
 */
public class Counter {

  /** Increment the state by 1 */
  public static State<Integer, Integer> increment() {
    return new State<>(state -> {
      int next = state + 1;
      return new State.Result<>(next, next);
    });
  }

  /** Add n to the state */
  public static State<Integer, Integer> add(int n) {
    return new State<>(state -> {
      int next = state + n;
      return new State.Result<>(next, next);
    });
  }
}