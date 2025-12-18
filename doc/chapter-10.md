# Chapter 10 — Free Monad Essentials

Free monads are often described as “an interpreter construction kit.”  
In the previous chapters we implemented monads directly—`Option`, `Either`, `Try`, `State`, `IO`, `Reader`, and `Writer`.  
All of them encode _a specific effect_ in their data structure.

But Free is different.

Free does **not** encode any effect at all.  
Instead, it encodes **structure**, leaving the _meaning_ of a program to be supplied later by an interpreter.

This separation—“structure first, semantics later”—is the conceptual heart of FP architecture, compiler pipelines, and DSL design. And remarkably, Java can implement it with a modest amount of code, thanks to sealed interfaces and records — but not without friction. Java’s type system still requires occasional @SuppressWarnings (raw/unchecked), which reflects inherent limitations rather than mistakes in the Free design.

## 10.1 What Is a Free Monad?

Intuitively:

- In a normal monad, `flatMap` _executes_ the next step.
- In a Free monad, `flatMap` merely _builds a tree_ representing the next step.

Free transforms any functor `F` into a monad `Free<F, A>` without defining the meaning of `F`.

```text
Functor F  ──(liftF)──▶  Free<F, A> (program description)
Interpreter ───────────▶  Result (meaning)
```

In other words:

**Free can be viewed as a universal AST builder for effectful programs.**

It gives you:

- a uniform structure (`Pure`, `Suspend`, `FlatMapped`)
- no commitment to semantics
- multiple interpreters for the same program  
  (evaluation, optimization, static analysis, codegen, visualization…)

This is why Free monads appear in:

- compilers
- interpreters
- algebraic effects
- structured logging
- workflow engines
- data pipelines
- SQL/IR design
- and all “Describe first, interpret later” systems

## 10.2 The Core Data Type

Our implementation consists of three constructors:

```java
public sealed interface Free<F, A> permits
    Free.Pure, Free.Suspend, Free.FlatMapped {

    record Pure<F, A>(A value) implements Free<F, A> {}

    record Suspend<F, A>(F fa) implements Free<F, A> {}

    record FlatMapped<F, A, B>(
            Free<F, A> sub,
            Function<A, Free<F, B>> k
    ) implements Free<F, B> {}
}
```

### Why these three?

- `Pure` represents a completed computation.
- `Suspend` wraps one step described by the functor `F`.
- `FlatMapped` captures sequencing: “run `sub`, then feed the result into `k`.”

This is the entire structure of a program written in Free.

## 10.3 Building Programs with `liftF`

The essential constructor is:

```java
static <F, A> Free<F, A> liftF(F fa) {
    return new Suspend<>(fa).flatMap(a -> new Pure<>(a));
}
```

This:

1. wraps an instruction `fa` into a suspended node
2. ensures the result type remains a proper monad (`Free<F, A>`)

`liftF` is how any DSL operation becomes a Free expression.

For example, in our arithmetic DSL:

```java
Free<ExprF, Integer> expr =
    AddF.of(lit(1), lit(2)).lift()
        .flatMap(x -> MulF.of(lit(x), lit(3)).lift());
```

This is **not executed** yet.  
Only a tree is built.

## 10.4 `map` and `flatMap` Build the AST

In normal monads:

- `map` transforms values
- `flatMap` _executes_ the next step

In Free:

- `map` wraps the continuation
- `flatMap` creates a **new tree node (`FlatMapped`)**

Thus a program like:

```java
expr.flatMap(f).flatMap(g).flatMap(h)
```

becomes a right-associated tree:

```java
FlatMapped(
  sub = expr,
  k   = x -> FlatMapped(
            sub = f(x),
            k   = y -> FlatMapped(
                       sub = g(y),
                       k   = h
                   )
        )
)
```

A future interpreter can traverse and normalize this tree however it wants.

## 10.5 Interpreting Free: Step-by-Step Evaluation

The interpreter receives:

- a Free program
- an `F → A` function describing what a single instruction means

Evaluation is:

1. Pattern-match on `Pure`, `Suspend`, or `FlatMapped`
2. Handle each case
3. Perform tail-recursive stepping with a small stack

Pseudo-logic:

```java
while (true) {
    switch (node) {
        case Pure(a) -> return a;
        case Suspend(fa) -> return interpret(fa);
        case FlatMapped(sub, k) -> {
            if (sub is Pure)
                node = k.apply(sub.value());
            else if (sub is Suspend)
                node = k.apply(interpret(sub.fa()));
            else // sub is FlatMapped
                node = re-associate to right
        }
    }
}
```

This evaluator:

- never grows the call stack
- normalizes left-nested binds
- provides predictable evaluation order

## 10.6 Why Free Matters for DSL Design

Free achieves something extremely valuable:

**The DSL designer defines only syntax.  
The interpreter author defines semantics later.**

Meaning:

- Want evaluation? Implement `ExprF → Integer`.
- Want optimization? Implement `ExprF → ExprF`.
- Want code generation? Implement `ExprF → String`.
- Want type checking? Implement `ExprF → Either<TypeError, Type>`.

All from the same Free program.

This separation is _the_ foundation of compiler design:

- AST → Optimization passes → Codegen

And Free naturally embodies this pipeline in Java.

## 10.7 When Should You Use Free?

Use Free when:

- You want a DSL whose semantics may change
- You want multiple backends (evaluation, optimization, SQL gen, etc.)
- You want to record the program structure
- You want programs as _data_ instead of executable logic

Avoid Free when:

- Only one simple effect is needed
- Performance is the primary concern
- You do not need an explicit AST

## 10.8 Summary

Key points:

1. Free builds program structure, not semantics.
2. `Pure`, `Suspend`, and `FlatMapped` form the entire AST.
3. `liftF` embeds DSL instructions into Free.
4. `map` and `flatMap` build trees instead of executing logic.
5. Interpreters supply meaning afterward.
6. Free is ideal for DSLs, IRs, and compiler-like architectures.

Chapter 11 will apply these ideas to our Expr DSL.
