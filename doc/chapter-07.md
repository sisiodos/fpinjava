# Chapter 07 — Tree and Structural Recursion

## 7.1 Trees as Algebraic Data Types

A tree is the canonical example of a recursively defined data structure.  
In functional programming, trees allow us to express hierarchical computation, symbolic expressions, and evaluation rules.

A binary tree can be defined using two constructors:

- **Leaf** — holds a single value
- **Branch** — holds two subtrees

This mirrors the inductive definition of a tree and supports structural recursion: computations follow the shape of the data.

## 7.2 Defining a Tree ADT in Java

With Java 21, we can express this ADT directly:

```java
public sealed interface Tree<A> permits Tree.Leaf, Tree.Branch {

    record Leaf<A>(A value) implements Tree<A> {}

    record Branch<A>(Tree<A> left, Tree<A> right) implements Tree<A> {}
}
```

This definition is:

- **closed** — sealed, no uncontrolled subclassing
- **exhaustive** — pattern matching can cover all variants
- **immutable** — records enforce immutability
- **structural** — matches the algebraic definition

## 7.3 Structural Recursion on Trees

Structural recursion is the process of:

1. Handling the base case (**Leaf**)
2. Recursively processing each subtree (**Branch**)

Example: computing the size of a tree

```java
public static <A> int size(Tree<A> tree) {
    return switch (tree) {
        case Tree.Leaf<A> l -> 1;
        case Tree.Branch<A> b -> 1 + size(b.left()) + size(b.right());
    };
}
```

A more general form is **fold**, analogous to `foldLeft` for lists:

```java
public static <A, B> B fold(
        Tree<A> tree,
        java.util.function.Function<A, B> leaf,
        java.util.function.BiFunction<B, B, B> combine
) {
    return switch (tree) {
        case Tree.Leaf<A> l -> leaf.apply(l.value());
        case Tree.Branch<A> b -> {
            B left  = fold(b.left(),  leaf, combine);
            B right = fold(b.right(), leaf, combine);
            yield combine.apply(left, right);
        }
    };
}
```

Examples:

```java
int sum   = fold(tree, a -> a, (l, r) -> l + r);
int depth = fold(tree, a -> 1, (l, r) -> 1 + Math.max(l, r));
boolean containsX = fold(tree, v -> v.equals("x"), (l, r) -> l || r);
```

`fold` cleanly separates the _shape of the data_ from the _logic of computation_.

## 7.4 Pattern Matching in Java 21

Java 21’s pattern matching makes Tree manipulation concise:

```java
static <A> boolean contains(Tree<A> t, A target) {
    return switch (t) {
        case Tree.Leaf<A> l -> l.value().equals(target);
        case Tree.Branch<A> b ->
            contains(b.left(), target) || contains(b.right(), target);
    };
}
```

Though not as expressive as Scala or Haskell, the ergonomics for ADT-style trees are good enough for many practical cases.

## 7.5 FP Trees vs the OOP Composite Pattern

Traditional Java represents trees using the Composite Pattern:

- abstract base class
- Leaf/Node subclasses
- virtual dispatch

FP takes a different view:

- data is a closed algebraic structure
- operations are _external_ and total
- pattern matching replaces polymorphic dispatch
- structural recursion replaces visitors

The result is simpler reasoning, clearer invariants, and better exhaustiveness guarantees.

## 7.6 Summary

Trees illustrate how **structural recursion** emerges naturally from algebraic data types.  
Java 21’s sealed types and pattern matching make these FP techniques directly accessible.

This chapter serves as a conceptual bridge to later chapters, where expression trees and Free monads extend this idea toward interpreter and compiler design.
