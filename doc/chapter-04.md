# Chapter 04 — The State Monad

In functional programming, the **State monad** provides a disciplined way to model computations that carry state along a sequence of operations, without mutating variables. This chapter explores how to express this concept in Java, using immutable data structures and pure functions.

## 4.1 What Problem Does the State Monad Solve?

Many algorithms require some notion of _state_: counters, random number generators, accumulators, symbol tables, and so on.

In imperative Java, these are usually implemented through:

- mutable fields,
- shared objects,
- side‑effects embedded in methods.

From an FP perspective, these techniques obscure data flow and make reasoning difficult.  
The State monad provides a pure alternative:

- **State is passed explicitly**, never hidden.
- **Each computation returns both a value and a new state**.
- **Compositionality**: complex stateful logic is built from smaller steps.

## 4.2 The State Monad Definition in Java

We adopt the canonical signature:

```java
public record State<S, A>(Function<S, Tuple2<A, S>> run) {

    public <B> State<S, B> map(Function<A, B> f) {
        return new State<>(s -> {
            var result = run.apply(s);
            return Tuple.of(f.apply(result._1()), result._2());
        });
    }

    public <B> State<S, B> flatMap(Function<A, State<S, B>> f) {
        return new State<>(s -> {
            var result = run.apply(s);
            return f.apply(result._1()).run().apply(result._2());
        });
    }
}
```

Key points:

- `run` encapsulates a computation from state `S` to a pair `(A, S)`.
- `map` transforms the _value_ while keeping the state intact.
- `flatMap` chains computations, threading state from one step to the next.

This design preserves purity while allowing sequential composition.

## 4.3 A Minimal Example: A Counter

A common example is a simple counter that increments as we evaluate expressions.

```java
public static State<Integer, Integer> increment() {
    return new State<>(s -> Tuple.of(s, s + 1));
}
```

Using `map` and `flatMap`, we can chain operations:

```java
State<Integer, Tuple2<Integer, Integer>> program =
    increment().flatMap(a ->
    increment().map(b ->
        Tuple.of(a, b)
    ));
```

Running:

```java
var result = program.run().apply(0);
// result = ((0, 1), state = 2)
```

This illustrates the fundamental rule:

> **State flows purely through the computation without mutation.**

## 4.4 Modeling Stateful Algorithms

The State monad becomes powerful when modeling logic such as:

- recursive tree traversals that carry contextual data,
- random number generators (RNG),
- symbol table construction during parsing,
- intermediate representations in compiler pipelines.

For example, a pseudo‑random generator:

```java
State<Long, Long> nextLong = new State<>(seed -> {
    long newSeed = seed * 6364136223846793005L + 1;
    return Tuple.of(newSeed, newSeed);
});
```

Because both the value and the new seed are explicit, the computation is:

- reproducible,
- testable,
- free of hidden side-effects.

## 4.5 Design Notes for Java

The State monad turns out to be one of the least problematic FP constructs to express in modern Java.
Because its structure is simple—`S → (A, S)`—Java 21 provides all the tools needed:

- records for lightweight immutable data containers,
- lambdas for pure computations,
- pattern matching for readability (when needed).

## ✔ Minimal friction

Unlike abstractions such as `Free`, or type-class–style polymorphism, **State does not require higher-kinded types**. Java’s parametric types are entirely sufficient, because State fixes the state parameter S and varies only the result type A.
This means:

- no encoding tricks,
- no unsafe casts,
- no boilerplate beyond what purity itself requires.

In practice, writing and composing State computations in Java 21 feels close to their Scala equivalents.

## ✔ What HKTs would enable—but are not required here

Higher-kinded types become relevant only when we want:

- a unified abstraction over all monads,
- generic combinators parameterized by a type constructor `F<\_>`,
- a type-class-like syntax for Monad, Applicative, etc.

However, **none of these are necessary to use State itself**.
For expressing deterministic state-threading in real-world algorithms (parsers, interpreters, random generators), Java’s type system is perfectly adequate.

## ✔ Remaining verbosity

The main friction points are incidental:

- Java lambdas remain more verbose than Haskell/Scala,
- Tuple types must be provided manually,
- Lack of partial application requires nested lambdas.

These are ergonomic issues, not structural limitations.

## Summary

The State monad offers:

- predictable, explicit state threading,
- referential transparency,
- strong compositional properties.

It is particularly instructive when studying interpreter construction, compiler pipelines, or any domain where state must be tracked without sacrificing purity.
