# Chapter 06 — Functional Data Structures: List

In this chapter we introduce _functional data structures_ in their canonical form.
While Java provides `List`, `ArrayList`, and `LinkedList`, none of these are _functional_:
they are mutable, operationally defined, and do not express structure in types.

A functional `List` is defined by _two constructors only_:

- `Nil` — the empty list
- `Cons(head, tail)` — an element followed by the rest of the list

This is the same structure as in Haskell or Scala, and underlies structural recursion.

## 6.1 The Algebraic Data Type

```java
public sealed interface List<A> permits List.Nil, List.Cons {

    record Nil<A>() implements List<A> {}

    record Cons<A>(A head, List<A> tail) implements List<A> {}
}
```

This gives us:

- **Immutability by design**
- **Total structural description** of the data
- A foundation for **structural recursion**

Compared to Java’s built‑in collections, the important difference is _the absence of hidden mutation_.
All operations return _new_ lists rather than modifying existing ones.

## 6.2 Basic Operations

The classical operations are:

- `prepend`
- `map`
- `foldRight` and `foldLeft`
- `append`
- `reverse`

### Example: map

```java
public static <A, B> List<B> map(List<A> list, Function<A, B> f) {
    return switch (list) {
        case List.Nil<A> ignored -> new List.Nil<>();
        case List.Cons<A> cons   -> new List.Cons<>(f.apply(cons.head()), map(cons.tail(), f));
    };
}
```

Because the ADT is small and total, recursion is exhaustive — covering all possible cases — making missing cases structurally impossible.

## 6.3 Folding as the Core Abstraction

`foldRight` expresses the essence of structural recursion:

```java
public static <A, B> B foldRight(List<A> list, B z, BiFunction<A, B, B> f) {
    return switch (list) {
        case List.Nil<A> ignored -> z;
        case List.Cons<A> cons   -> f.apply(cons.head(), foldRight(cons.tail(), z, f));
    };
}
```

Many operations—including `map`, `length`, and `append`—can be implemented via folds.

This is the “functional data structure lesson”:  
**data type dictates recursion scheme**.

## 6.4 Performance Considerations

Functional lists have:

- **O(1)** prepend
- **O(n)** append
- **O(n)** indexing

Unlike Java’s `LinkedList`, our ADT has _no hidden machinery_ and _no mutability_.
The tradeoff is intentional: structural clarity over operational tuning.

In FP languages, lists serve as _semantic glue_: they appear in interpreters, compilers, symbolic computing, and DSLs.

## 6.5 Why This Matters for FP in Java

Java 21’s sealed types make this pattern much more natural:

- Exhaustive `switch` becomes safe
- ADTs become explicit instead of accidental
- Recursion is clearer and less error‑prone

Even so, Java’s pattern-matching remains less expressive than that of Scala, Kotlin, or Haskell, and its type system does not support higher-kinded abstraction.
Within these constraints, however, the core ideas translate well,
and the resulting code remains surprisingly _clean_.

---

Next we extend these ideas to **Tree structures** and **structural recursion**, which generalizes the same principles to branching shapes.
