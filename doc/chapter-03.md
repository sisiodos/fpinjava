# Chapter 03 - Option / Either / Try — Algebraic Data Types in a Nominal World

Algebraic Data Types (ADTs) form the basic vocabulary of functional programming.  
They allow us to _classify_ computations by their possible shapes, and they allow the compiler to enforce these shapes.  
In this chapter, we reconstruct three essential ADTs in Java:

- `Option<A>` — presence or absence
- `Either<L, R>` — branching structure
- `Try<A>` — exception-safe computation

Although these constructs are simple, they expose the core tension between FP semantics and Java’s nominal type system.

## 3.1 What ADTs Represent

An ADT is defined not by its methods, but by its _constructors_.  
`Option<A>` is the sum of two cases:

```java
Option<A> = Some(a) | None
```

Similarly:

```java
Either<L, R> = Left(l) | Right(r)
Try<A>      = Success(a) | Failure(e)
```

Each of these structures encodes a _logical space_ of possibilities.  
Pattern matching allows us to make this structure explicit.

## 3.2 Expressing ADTs in Java 17

Java 17 supports records and sealed interfaces, enabling a minimal ADT style:

```java
public sealed interface Option<A> permits Some, None {}

public record Some<A>(A value) implements Option<A> {}
public record None<A>()        implements Option<A> {}
```

Conceptually, this is close to Scala, but several limitations remain:

- Generics are erased at runtime.
- Exhaustiveness checking is partial.
- Pattern matching on `switch` is not fully supported.

This results in some additional ceremony in fold/map/flatMap implementations, reflecting Java’s nominal type model, but the semantics remain clear and predictable.

## 3.3 Improvements in Java 21

With Java 21:

- Pattern matching is available in `switch`.
- Deconstruction patterns apply directly to records.
- The compiler can check exhaustiveness for sealed hierarchies.

This allows expressive FP-style code:

```java
return switch (opt) {
  case Some(var v) -> f.apply(v);
  case None _      -> defaultValue;
};
```

ADTs become more _transparent_, though not as concise as in FP-native languages.

## 3.4 Option — Modeling Optionality Explicitly

### Intent

`Option<A>` explicitly represents _absence_, removing the need for `null`.

### Essential operations

- `map` — transform inside the container
- `flatMap` — sequence optional computations
- `getOrElse` — recover default values
- `fold` — deconstruct the ADT (core abstraction)

### Example

```java
Option<Integer> x = some(10)
    .map(v -> v + 1)
    .flatMap(v -> v > 10 ? some(v) : none());
```

### Interpretation

`Option` expresses _non-failure_ absence.  
It is appropriate when “no value” is a valid, expected result—not an error condition.

## 3.5 Either — Expressing Structured Alternatives

`Either<L, R>` carries _two_ possible shapes:

- `Left<L>` typically represents error or alternative path
- `Right<R>` represents success or primary path

This ADT is essential for:

- parser combinators
- branch-sensitive computations
- state machines
- interpreter pipelines

### Example

```java
Either<String, Integer> result =
    parseInt("10")
      .map(i -> i + 1)
      .flatMap(i -> i < 20
          ? Either.right(i)
          : Either.left("Too large"));
```

### Interpretation

`Either` exposes branching logic explicitly, making error paths _visible in the type system_.  
This is a fundamental FP discipline: _errors are values, not exceptions_.

## 3.6 Try — Controlled Exceptional Computation

`Try<A>` captures Java exceptions in a pure ADT.

### Motivation

Java’s built-in exception mechanism is:

- invisible in the type system
- non-compositional
- side-effecting

`Try` restores structure:

```java
Try<A> = Success(a) | Failure(exception)
```

### Example

```java
Try<Integer> t =
    Try.of(() -> Integer.parseInt("10"))
       .map(n -> n + 1)
       .recover(e -> 0);
```

### Interpretation

`Try` is appropriate when exceptions must be handled _locally_ and _composed_ across transformations.

## 3.7 What These ADTs Reveal About Java

Before examining the limitations, it is worth emphasizing a positive and perhaps surprising result: **Option, Either, and Try are pleasant to use in Java 21.** With sealed interfaces, records, and pattern matching, these ADTs impose very little cognitive or syntactic burden. For many programs, especially those that do not attempt to abstract over higher-kinded types, Java 21 provides a sufficiently expressive substrate. In other words: **if one stays at the level of concrete ADTs and does not pursue type-level abstraction, Java’s ergonomics are entirely acceptable.**

Implementing these ADTs in Java clarifies several important points:

1. **`fold` is the central operation**
   In Java 17, `fold` was the primary way to make the structure of an ADT explicit. Without pattern matching on `switch`, all structural recursion had to be expressed through library-level abstractions such as `fold`, `map`, and `flatMap`. In other words, `fold` carried the full weight of case analysis.

2. **FP relies on data constructors and explicit case analysis**
   In Java’s nominal type system, this structure must be emulated explicitly, as generic variance and subtype relationships (for example, `? super` / `? extends`) do not naturally encode such cases.

3. **Java does not provide higher‑kinded types**, which means abstractions such as Functor and Monad must be expressed explicitly rather than derived from the type system.

4. **Pattern matching substantially narrows the gap**, though Java’s design remains oriented toward object‑centric composition.

5. **Error handling becomes explicit**, which is a significant improvement over exceptions.

## 3.8 Summary

Option, Either, and Try form the conceptual foundation of all later chapters:

- parser combinators rely on Either
- State and IO rely on map/flatMap semantics
- Free monads rely on ADTs to define instruction sets
- DSL interpreters rely on structural recursion over ADTs

Understanding these data structures in Java clarifies the broader thesis of this book:

> **FP is the art of structuring computation through algebraic data types.  
> Java approaches this style with its own tools and trade‑offs, offering a practical—if distinct—path toward FP‑influenced design.**
