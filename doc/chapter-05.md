# Chapter 05 — IO and Effect Suspension

Modern Java (particularly Java 21) offers enough expressive power to encode the essential FP effect patterns—IO, Reader, and Writer—without significant friction.  
Records, lambdas, and pattern matching collectively allow these constructs to be implemented in a direct and readable manner.  
This chapter examines how these effect abstractions work, why they matter, and how Java can express them cleanly.

## 5.1 Why effects must be suspended

A pure function always returns the same value for the same input.  
Side effects—printing to the console, reading a file, mutating state—break this property because they depend on _time_ and _context_.

FP does not forbid effects. Instead, it enforces _discipline_:

- effects must be **described**, not executed immediately
- effects must be **composed** before execution
- effects must have **one well-defined execution point**

With IO, effects become referentially transparent: two identical IO values behave identically as descriptions of effects.

## 5.2 IO — Suspending effects

In Java 21, IO can be defined compactly using a record:

```java
public record Io<A>(Supplier<A> thunk) {
    public A unsafeRun() { return thunk.get(); }

    public <B> Io<B> map(Function<? super A, ? extends B> f) {
        return new Io<>(() -> f.apply(unsafeRun()));
    }

    public <B> Io<B> flatMap(Function<? super A, Io<B>> f) {
        return new Io<>(() -> f.apply(unsafeRun()).unsafeRun());
    }
}
```

This structure closely mirrors IO implementations found in Scala or Haskell.

### Why Java 21 works well here

- `record` enables concise immutable wrappers
- Lambdas naturally defer evaluation
- `map` and `flatMap` create sequential effect chains
- No higher-kinded types are needed

IO in Java 21 is therefore _practical_, expressive, and conceptually aligned with standard FP semantics.

## 5.3 Reader — Environment-passing computations

Reader represents computations that depend on some environment:

```text
R → A
```

It offers an alternative to DI frameworks, favoring a simpler, explicit, and type-directed approach.

```java
public record Reader<R, A>(Function<R, A> run) {
    public A apply(R env) { return run.apply(env); }

    public <B> Reader<R, B> map(Function<? super A, ? extends B> f) {
        return new Reader<>(env -> f.apply(run.apply(env)));
    }

    public <B> Reader<R, B> flatMap(Function<? super A, Reader<R, B>> f) {
        return new Reader<>(env -> f.apply(run.apply(env)).run.apply(env));
    }

    public static <R> Reader<R, R> ask() {
        return new Reader<>(env -> env);
    }

    public <B> Reader<R, B> local(Function<R, R> modify) {
        return new Reader<>(env -> run.apply(modify.apply(env)));
    }
}
```

### Why Reader fits Java well

- No higher-kinded types required
- Very close to its Scala/Haskell counterparts
- Expressive for configuration passing
- Eliminates need for containers or global state

Reader is a strong example of FP abstractions fitting naturally within Java’s syntax and type system.

## 5.4 Writer — Accumulating logs

Writer captures a value plus an accumulated log:

```text
(A, W)
```

Its Java 21 representation is compact:

```java
public record Writer<W, A>(A value, W log, BinaryOperator<W> append) {
    public <B> Writer<W, B> map(Function<? super A, ? extends B> f) {
        return new Writer<>(f.apply(value), log, append);
    }

    public <B> Writer<W, B> flatMap(Function<? super A, Writer<W, B>> f) {
        Writer<W, B> next = f.apply(value);
        return new Writer<>(next.value, append.apply(log, next.log), append);
    }
}
```

### Why Writer also works well in Java

- Java collections (e.g., `List<String>`) naturally serve as logs
- `record` eliminates boilerplate
- `append` abstracts monoid behavior without type classes

Writer shows that Java can express FP accumulation patterns clearly and succinctly.

## 5.5 The shared structure: effectful computation as data

IO, Reader, and Writer share a unifying idea:

**Effectful computations should be represented as values that can be combined before they are executed.**

In Java 21, this model works smoothly:

- pure wrappers (`record`)
- first-class functions (`Function`, lambdas)
- local reasoning via `map` and `flatMap`
- explicit execution boundaries

These abstractions align cleanly with functional semantics while remaining practical for real-world Java development.

## 5.6 Summary

Java 21 provides a sufficiently expressive foundation for effect suspension.  
IO, Reader, and Writer can be written with minimal ceremony, without advanced type-system features, and without deviating from their functional meaning.

These constructs complete the fundamental FP tools needed before moving on to more advanced abstractions such as parsers, interpreters, and—later in the book—free monads.
