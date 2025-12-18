package org.example.app.fp.demo.io;

import org.example.app.fp.core.Unit;
import org.example.app.fp.effect.Io;

public class IoDemo {
  public static void main(String[] args) {
    // scanner
    // Io<java.util.Scanner> scanner = Io.of(() -> new java.util.Scanner(System.in));

    // IO that reads user input
    // Using bracket to acquire and release the resource
    Io<String> program = Io.of(() -> new java.util.Scanner(System.in)).bracket(
        scanner -> Io.of(() -> {
          System.out.println("What is your name?");
          return scanner.nextLine();
        }),
        // close resource
        // This is not elegant... but required due to Java's type system
        scanner -> Io.of(() -> {
          scanner.close();
          return null; // Returning null for Io<Void>; unavoidable in Java
        }))
        .map(name -> "Hello, " + name);

    // Version that explicitly uses Unit type
    Io<String> program2 = Io.of(() -> new java.util.Scanner(System.in)).bracket(
        scanner -> Io.of(() -> {
          System.out.println("What is your name?");
          return scanner.nextLine();
        }),
        // close resource
        // Introduced Unit type
        scanner -> Io.of(() -> {
          scanner.close();
          return Unit.INSTANCE;
        }))
        .map(name -> "Hello, " + name);

    // Simpler version using Consumer
    Io<String> program3 = Io.of(() -> new java.util.Scanner(System.in)).bracketWithRelease(
      scanner -> Io.of(() -> {
        System.out.println("What is your name?");
        return scanner.nextLine();
      }), java.util.Scanner::close
    ).map(name -> "Hello, " + name);

    // Even simpler version using AutoCloseable
    Io<String> program4 = Io.using(
      Io.of(() -> new java.util.Scanner(System.in)),
      scanner -> Io.of(() -> {
        System.out.println("What is your name?");
        return scanner.nextLine();
      })
    ).map(name -> "Hello, " + name);

    // Execute IO (lazy until run) to produce side effects
    System.out.println(program.run());
    System.out.println(program2.run());
    System.out.println(program3.run());
    System.out.println(program4.run());
  }
}
