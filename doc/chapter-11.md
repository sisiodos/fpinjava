# Chapter 11 — Designing an Expr DSL Using Free

In Chapter 10 we built the conceptual foundation of the Free monad.  
Now we will put that abstraction to work by designing a small arithmetic DSL.  
This chapter demonstrates how Free enables a _pure, declarative, and fully interpretable_ representation of computations — a miniature example of how real‑world compilers and IR pipelines are structured.

The goal is **not** to build a powerful expression language.  
Instead, it is to show how:

1. A DSL is defined using a functor (`ExprF`)
2. DSL instructions are lifted into `Free<ExprF, A>`
3. Programs become data structures (ASTs)
4. Interpreters and optimizers operate on those structures

This is the smallest possible version of a compiler pipeline written in Java.

## 11.1 Defining the Expression Functor `ExprF<A>`

The functor `ExprF<A>` represents _one step_ of computation in our DSL.  
We model four arithmetic operations and a literal:

```java
public sealed interface ExprF<A>
    permits ConstF, AddF, SubF, MulF, DivF, SumF {

    ExprF<?> map(Function<Object, Object> f);
}
```

Each instruction is a simple record:

```java
public record ConstF(Integer value) implements ExprF<Integer> {}

public record AddF<A>(A left, A right) implements ExprF<Integer> {}

public record SubF<A>(A left, A right) implements ExprF<Integer> {}

public record MulF<A>(A left, A right) implements ExprF<Integer> {}

public record DivF<A>(A left, A right) implements ExprF<Integer> {}

// A variadic node (e.g., addition of many terms)
public record SumF<A>(List<A> values) implements ExprF<Integer> {}
```

Each constructor contains **references to subexpressions**, but _not interpretations_.

The `map` method exists only because Free requires `ExprF` to behave like a functor.  
This method rewrites the child nodes but does **not** evaluate anything.

## 11.2 Lifting Instructions Into Free

Every DSL instruction must be lifted into a Free program:

```java
static <A> Free<ExprF, A> liftF(ExprF<A> fa) {
    return Free.liftF(fa);
}
```

We then define _smart constructors_ for convenience:

```java
static Free<ExprF, Integer> lit(int n) {
    return liftF(new ConstF(n));
}

static Free<ExprF, Integer> add(Free<ExprF, Integer> a,
                                Free<ExprF, Integer> b) {
    return a.flatMap(x -> b.flatMap(y ->
            liftF(new AddF<>(x, y))));
}

static Free<ExprF, Integer> sum(List<Free<ExprF, Integer>> xs) {
    return sequence(xs).flatMap(vals ->
            liftF(new SumF<>(vals)));
}
```

These smart constructors do not compute anything.  
They merely **build nodes** in a program tree.

## 11.3 Programs Are Now Pure Data (ASTs)

A simple program:

```java
var expr =
    add(lit(1), lit(2))
        .flatMap(x -> mul(lit(x), lit(3)));
```

This does **not** run arithmetic.  
It constructs a deeply nested `Free<ExprF, Integer>` value.  
A future interpreter will decide how this tree should execute.

Free has now turned program logic into **data**.

This is the essence of DSL design.

## 11.4 Visualizing the AST

Our previous example desugars into:

```java
FlatMapped(
  sub = Suspend(AddF(ConstF(1), ConstF(2))),
  k   = x -> Suspend(MulF(ConstF(x), ConstF(3))))
```

Free ensures:

- The order of operations is explicit
- The AST is structurally normalized
- No Java-side side‑effects occur during construction

This is precisely the kind of structure you want before writing an interpreter or optimizer.

## 11.5 Building Utility: sequence / traverse

Many DSLs require combining lists of subcomputations.  
We implement `sequence` using the Free monad’s own structure:

```java
static <A> Free<ExprF, List<A>> sequence(List<Free<ExprF, A>> xs) {
    Free<ExprF, List<A>> acc = Free.pure(new ArrayList<>());

    for (var x : xs) {
        acc = acc.flatMap(list ->
              x.flatMap(v -> {
                  list.add(v); // Although this uses a mutable list internally,
                               // the mutation is not observable
                               // outside the construction process.
                  return Free.pure(list);
              })
        );
    }
    return acc;
}
```

This lets us express a variadic `SumF` node easily.

## 11.6 What We Have Achieved

By defining:

- a _functor_ (`ExprF`)
- a _lifting function_ (`liftF`)
- _smart constructors_ (e.g., `lit`, `add`, `sum`)
- _program combinators_ (`flatMap`, `sequence`)

…we have built a **real DSL** inside Java.

This DSL:

- captures structure independently of meaning
- builds an AST naturally through monadic composition
- supports multiple interpreters (evaluation, optimization, codegen)
- mirrors compiler architecture on a small scale
- demonstrates the conceptual power of Free monads

In the next chapter, we provide these interpreters — thereby giving our DSL meaning.

## 11.7 Summary

1. `ExprF<A>` defines _one step_ of a computation.
2. Free turns these steps into a composable program.
3. Smart constructors make DSL usage ergonomic.
4. Programs become first‑class data (ASTs).
5. This separation of structure and semantics is the core of DSL/IR design.

Chapter 12 will interpret, optimize, and compile this DSL.
