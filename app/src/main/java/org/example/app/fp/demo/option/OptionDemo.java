package org.example.app.fp.demo.option;

import org.example.app.fp.core.Option;
import org.example.app.fp.core.Either;
import org.example.app.fp.core.Try;
import org.example.app.fp.core.Unit;

public class OptionDemo {

  public static void main(String[] args) {

    // ---------- Option demo ----------
    Option<Integer> maybeInt = Option.some(10);

    Option<Integer> plusFive = maybeInt.map(x -> x + 5);

    System.out.println("Option map: " + plusFive);

    // flatMap example
    Option<Integer> flat = maybeInt.flatMap(x -> (x > 5)
        ? Option.some(x * 2)
        : Option.none());

    System.out.println("Option flatMap: " + flat);

    Option<Integer> noneCase = Option.<Integer>none().map(x -> x + 1);
    System.out.println("None map: " + noneCase);

    // ---------- Either demo ----------
    Either<String, Integer> right = Either.right(42);
    Either<String, Integer> left = Either.left("Error!");

    System.out.println("Right map: " + right.map(x -> x + 1));
    System.out.println("Left map:  " + left.map(x -> x + 1)); // unchanged

    // fold
    String foldResult = right.fold(
        err -> "Left: " + err,
        ok -> "Right: " + ok);

    System.out.println("Either fold: " + foldResult);

    // ---------- Try demo ----------
    Try<Integer> success = Try.of(() -> 10 / 2);
    Try<Integer> failure = Try.of(() -> 1 / 0);

    System.out.println("Try success: " + success);
    System.out.println("Try failure: " + failure);

    // Try.map propagates failure
    System.out.println("Try map success: " + success.map(x -> x * 3));
    System.out.println("Try map failure: " + failure.map(x -> x * 3));

    // ---------- Unit demo ----------
    // Unit is used when the value is meaningless (only effect matters)
    @SuppressWarnings({ "unused" })
    Unit u = Unit.INSTANCE;

    // a "procedure-like" FP function
    Runnable printHello = () -> {
      System.out.println("Hello!");
    };

    System.out.println("Unit demo:");
    printHello.run();
  }
}