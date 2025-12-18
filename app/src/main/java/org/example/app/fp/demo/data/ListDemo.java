package org.example.app.fp.demo.data;

import org.example.app.fp.data.List;

/**
 * Demonstrates usage of the functional List implementation.
 * Shows map, filter, flatMap, folds, and structural recursion.
 */
public final class ListDemo {

  public static void main(String[] args) {

    // ------------------------------------------------------------------
    // Constructing lists
    // ------------------------------------------------------------------
    List<Integer> xs = List.of(1, 2, 3, 4, 5);
    System.out.println("xs = " + xs);

    List<Integer> ys = List.cons(10, List.cons(20, List.empty()));
    System.out.println("ys = " + ys);

    // ------------------------------------------------------------------
    // map
    // ------------------------------------------------------------------
    var doubled = xs.map(x -> x * 2);
    System.out.println("map (*2): " + doubled);

    // ------------------------------------------------------------------
    // filter
    // ------------------------------------------------------------------
    var evens = xs.filter(x -> x % 2 == 0);
    System.out.println("filter even: " + evens);

    // ------------------------------------------------------------------
    // flatMap
    // ------------------------------------------------------------------
    var expanded = xs.flatMap(x -> List.of(x, -x));
    System.out.println("flatMap expand: " + expanded);

    // ------------------------------------------------------------------
    // append / concat
    // ------------------------------------------------------------------
    var appended = xs.append(ys);
    System.out.println("append xs ++ ys: " + appended);

    // ------------------------------------------------------------------
    // folds
    // ------------------------------------------------------------------
    int sum = xs.foldLeft(0, (acc, a) -> acc + a);
    System.out.println("sum via foldLeft: " + sum);

    // ------------------------------------------------------------------
    // length, reverse, exists, forAll
    // ------------------------------------------------------------------
    System.out.println("length: " + xs.length());
    System.out.println("reverse: " + xs.reverse());
    System.out.println("exists > 3? " + xs.exists(x -> x > 3));
    System.out.println("forAll < 10? " + xs.forAll(x -> x < 10));

    // ------------------------------------------------------------------
    // take / drop
    // ------------------------------------------------------------------
    System.out.println("take(3): " + xs.take(3));
    System.out.println("drop(2): " + xs.drop(2));

    // ------------------------------------------------------------------
    // toJavaList (interop)
    // ------------------------------------------------------------------
    System.out.println("Java list: " + xs.toJavaList());
  }
}