# Functional Programming in Java

## Summary

This repository accompanies a book-length experiment on practicing
functional programming in modern Java, primarily Java 21.

It is not an attempt to turn Java into Scala or Haskell.
Nor is it a tutorial on functional syntax or library usage.

Instead, this work explores a simple question:

**What kind of functional programming becomes possible
when Java is pushed to the edge of its current language design?**

Recent versions of Java have quietly accumulated features
that significantly change how programs can be structured:

- immutable data via `record`
- algebraic-style modeling via `sealed` types
- increasingly expressive pattern matching
- improved type inference and lambda ergonomics

Taken together, these features make Java closer to
functional languages than it has ever been before.

This book treats Java as a _constrained functional language_,
and examines what can be built within those constraints.

## Motivation

Functional programming is often taught using languages that were designed for it from the beginning.
That is valuable—but it makes it easy to miss why those abstractions exist at all.
This project takes the opposite approach.

By attempting functional programming **inside Java**,
we are forced to confront design pressure directly:
what the language enables, what it resists, and where abstractions begin to leak.

The goal is **clarity** rather than convenience.

Implementing core functional constructs in Java—
such as `Option`, `Either`, `State`, `IO`, parser combinators, and even a minimal Free Monad—
reveals why these abstractions exist and what structural problems they are meant to solve.

Java makes these problems visible precisely because it does not erase them with syntax.

## Why Java 21?

Java 21 represents a turning point—not as a sudden break,
but as the point where a long sequence of changes becomes coherent.

Java’s path toward functional programming did not begin with Java 21.

Java 8 introduced functional interfaces and lambda expressions,
making it practical to pass functions into other functions in everyday Java code.

Java 17 added records and sealed interfaces,
making it possible to model immutable data and closed sets of variants explicitly—
a prerequisite for functional-style data modeling.

With Java 21, pattern matching integrates these features at the syntactic level.
Data modeling, branching, and decomposition now work together,
making functional-style control flow practical in Java.

Taken together, these changes make it possible
to treat modern Java as a constrained functional language—
not by accident, but by gradual design.

## Scope of the Experiment

This book progresses from basic functional data types
to effect modeling and DSL construction:

- foundational types (`Option`, `Either`, `Try`)
- state and effect suspension
- functional data structures
- parser combinators
- Free Monads and interpretation
- DSL design and the boundary between programs and data

The emphasis is not on performance or completeness, but on **structural insight**.
Code exists to support reasoning, and reasoning exists to expose structure.
The code is not presented as a solution, but as a concrete artifact through which structure becomes visible.

Rather than bringing Java closer to functional languages,
this experiment uses Java to make the hidden structure of functional languages easier to see.
