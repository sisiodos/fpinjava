package org.example.app.fp.demo.state;

import org.example.app.fp.effect.State;

public class StateDemo {
  public static void main(String[] args) {

    // Build a stateful computation purely by composing functions
    State<Integer, Integer> program = 
        Counter.increment()                        // +1
        .flatMap(_ignored -> Counter.add(10))    // +10
        .flatMap(_ignored -> Counter.increment()); // +1

    System.out.println("Initial State: 0");

    // Execute the stateful computation with initial state = 0
    State.Result<Integer, Integer> result = program.run(0);

    System.out.println("Final Value: " + result.value());
    System.out.println("Final State: " + result.state());
  }
}