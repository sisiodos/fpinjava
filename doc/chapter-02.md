# Chapter 02 - Java 17 vs Java 21 — An FP-Oriented Comparison

Recent versions of Java introduce language features that make the language noticeably more hospitable to functional programming patterns.  
However, the difference between “possible” and “comfortable” functional programming is significant.  
This chapter clarifies what each version offers, and what functional abstractions become easier or remain awkward.

## 1. ADT Capability: Sealed Types vs. Open Hierarchies

### Java 17

- Introduces `record` and `sealed` classes.
- Algebraic Data Types (ADTs) can be emulated, but require boilerplate.
- Exhaustiveness is _not_ enforced unless pattern matching is used carefully.

### Java 21

- Fully supports sealed hierarchies with pattern matching for both `instanceof` and `switch`.
- ADT definitions become closer to their Scala/Haskell equivalents.
- Compiler can enforce totality across patterns.

### FP Impact

- Core FP constructs such as `Option`, `Either`, `Try`, `List`, and `Tree` map naturally to ADTs.
- Java 21 reduces syntactic friction, but deeper semantic FP features remain absent.

## 2. Pattern Matching: Toward Expression-Oriented Code

### Java 17

- Pattern matching is limited to `instanceof`.
- Decomposition requires manual casting or visitor patterns.
- FP-style recursion is verbose.

### Java 21

Pattern-switch expressions allow expressive decomposition:

```java
return switch (opt) {
  case Some(var v) -> f(v);
  case None _      -> defaultValue;
};
```

### FP Impact

- Removes a large amount of ceremony.
- Enables clear interpreter and DSL implementations.
- Shifts Java closer to an expression-oriented language, a key requirement for FP.

## 3. Records and Immutability

### Java 17

- `record` provides concise immutable data structures.
- Plays well with FP-style case classes.
- Immutability is shallow, but adequate for ADTs.

### Java 21

- Integrates cleanly with pattern matching and sealed types.

### FP Impact

- Records act as constructors for ADT nodes.
- Combined with `sealed`, they form the foundation for FP modeling in Java.

## 4. How These Features Enable FP Constructs

Java 21 enables:

- Expressive ADT modeling
- Pure interpreter-style programming
- Safer recursion via exhaustive pattern matching
- Declarative, expression-based computation

These limitations are not accidental.

Java is fundamentally designed as an object-oriented, imperative language.
Its type system, syntax, and standard library are optimized for explicit control flow,
nominal typing, and long-term backward compatibility.

From this perspective, it is natural that features such as:

- higher-kinded types
- FP-oriented type inference
- monadic syntax
- comprehensive functional standard library

fall outside Java’s core design priorities.

## Summary

Java 21 brings the language significantly closer to an FP-friendly environment,  
but the underlying model remains object-oriented and nominally typed.  
These additions enable functional-style programming, without changing Java’s conceptual center.

Its improvements matter for pedagogy and DSL construction,  
yet they do not turn Java into a functional programming language.

This sets the stage for the next chapters: understanding how FP abstractions  
can be expressed _despite_ the host language, and what we learn from those attempts.
