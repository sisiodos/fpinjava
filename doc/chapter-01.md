# Chapter 01 - Introduction

## Why Study Functional Programming _in Java_?

Recent versions of Java have introduced records, sealed types, and pattern matching.
These features make the language appear closer to modern functional languages.
At the beginning of this project, I held the expectation that:

> _“Perhaps Java is now expressive enough to support functional programming without friction.”_

This expectation was reasonable — and incorrect.

Java can emulate certain patterns from functional languages,
but it cannot offer the semantic foundations on which functional programming is built.
Higher-kinded types do not exist, subtyping dominates the type system,
and generics are erased at runtime.
These constraints limit what can be expressed cleanly.

However, this limitation became the starting point of the book.

Attempting to write functional programs _within_ Java’s boundaries reveals something essential:

> Functional programming is not merely a collection of idioms.
> It is better understood as a discipline centered on algebraic data types,
> pure composition, and interpretable program structures.

Java’s resistance to these ideas makes the underlying principles easier—not harder—to see.

Throughout this book, we explore several core functional abstractions:

- algebraic data types (Either, Option, Try)
- immutable data structures
- State and IO as explicit effects
- parser combinators
- Free monads and interpreters
- domain-specific languages and intermediate representations

Each abstraction exposes the same lesson:

> **Functional programming is a way to _describe computations_,
> not a way to organize classes.**

This book is therefore not an argument for writing functional programs in Java.
Rather, it uses Java intentionally as a constrained environment
in which the structure of functional programming becomes clearer.

If this book succeeds, you will not conclude that Java is a good choice for functional programs.  
You will instead understand _why_ functional languages are designed differently,  
and why those designs matter.
