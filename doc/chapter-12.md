# Chapter 12 — Interpretation, Optimization, and Code Generation

In Chapters 10 and 11, we built:

- a functor describing expression syntax (`ExprF`)
- a Free monad creating programs as ASTs
- a set of smart constructors (`const`, `add`, `mul`, …)

We now complete the compiler pipeline by giving our DSL _meaning_.

This chapter implements:

1. **Evaluation (Interpreter)**
2. **Optimization (Constant Folding)**
3. **Code Generation (pretty-printing / Java-like source)**

These three phases demonstrate how a Free-based DSL naturally supports  
**multiple semantics without changing the DSL itself**.

## 12.1 Revisiting the ExprF Constructors

Our expression language now consists of:

```java
public sealed interface ExprF<A>
    permits ConstF, AddF, SubF, MulF, DivF, SumF {
    ExprF<?> map(Function<Object,Object> f);
}

public record ConstF(Integer value) implements ExprF<Integer> {}

public record AddF<A>(A left, A right) implements ExprF<Integer> {}
public record SubF<A>(A left, A right) implements ExprF<Integer> {}
public record MulF<A>(A left, A right) implements ExprF<Integer> {}
public record DivF<A>(A left, A right) implements ExprF<Integer> {}

public record SumF<A>(List<A> values) implements ExprF<Integer> {}
```

These constructors describe **syntax only**.  
They say nothing about how expressions should be evaluated or optimized.

This is exactly what makes Free powerful.

## 12.2 Interpreter: Evaluating ExprF into Integer

```java
public final class ExprInterpreter {

    public static Integer eval(Free<ExprF, Integer> program) {
        return step(program);
    }

    private static Integer step(Free<ExprF, Integer> node) {
        while (true) {
            if (node instanceof Free.Pure<ExprF, Integer> p) {
                return p.value();
            }

            if (node instanceof Free.Suspend<ExprF, Integer> s) {
                return evalF(s.fa());
            }

            if (node instanceof Free.FlatMapped<ExprF, ?, Integer> fm) {
                var sub = fm.sub();
                var k = fm.k();

                if (sub instanceof Free.Pure<ExprF, ?> sp)
                    node = k.apply(sp.value());

                else if (sub instanceof Free.Suspend<ExprF, ?> ss)
                    node = k.apply(evalF(ss.fa()));

                else if (sub instanceof Free.FlatMapped<ExprF, ?, ?> sfm)
                    node = sfm.sub().flatMap(
                        x -> sfm.k().apply(x).flatMap(fm.k())
                    );

                else
                    throw new IllegalStateException("Unexpected Free node");
            }
        }
    }

    private static Integer evalF(ExprF<?> f) {
        return switch (f) {
            case ConstF c -> c.value();
            case AddF<?> a -> ((Integer) a.left()) + ((Integer) a.right());
            case SubF<?> a -> ((Integer) a.left()) - ((Integer) a.right());
            case MulF<?> a -> ((Integer) a.left()) * ((Integer) a.right());
            case DivF<?> a -> ((Integer) a.left()) / ((Integer) a.right());
            case SumF<?> s -> ((List<Integer>) s.values())
                                .stream().reduce(0, Integer::sum);
        };
    }
}
```

### What this shows

- Evaluation semantics are defined _after_ DSL definition.
- DSL programs remain pure data structures.
- Interpreters are replaceable and composable.

This is exactly the separation found in real-world compilers.

## 12.3 Optimization: Constant Folding

```java
public final class ExprOptimizer {

    public static Free<ExprF, Integer> optimize(Free<ExprF, Integer> program) {
        return program.resume()
            .fold(
                ExprOptimizer::optimizeF,
                Free::pure
            );
    }

    private static Free<ExprF, Integer> optimizeF(ExprF<?> f) {
        return switch (f) {
            case ConstF c -> Free.pure(c.value());

            case AddF<?> a -> {
                var l = (Integer) a.left();
                var r = (Integer) a.right();
                yield Free.pure(l + r);
            }

            case MulF<?> m -> {
                var l = (Integer) m.left();
                var r = (Integer) m.right();
                yield Free.pure(l * r);
            }

            default -> Free.liftF((ExprF<Integer>) f);
        };
    }
}
```

### Observations

- Optimization is purely structural.
- Side-effect–free design makes rewrites trivial.
- More passes can be added without touching the DSL.

This is how compilers like LLVM organize transform pipelines.

## 12.4 Code Generation: Turning ExprF into a String

```java
public final class ExprCodegen {

    public static String toSource(Free<ExprF, Integer> program) {
        return render(program);
    }

    private static String render(Free<ExprF, Integer> node) {
        if (node instanceof Free.Pure<ExprF, Integer> p)
            return p.value().toString();

        if (node instanceof Free.Suspend<ExprF, Integer> s)
            return renderF(s.fa());

        if (node instanceof Free.FlatMapped<ExprF, ?, Integer> fm) {
            throw new IllegalStateException(
                "FlatMapped should be normalized before codegen"
            );
        }

        throw new IllegalStateException("Unexpected Free node");
    }

    private static String renderF(ExprF<?> f) {
        return switch (f) {
            case ConstF c -> c.value().toString();
            case AddF<?> a -> "(" + a.left() + " + " + a.right() + ")";
            case SubF<?> a -> "(" + a.left() + " - " + a.right() + ")";
            case MulF<?> a -> "(" + a.left() + " * " + a.right() + ")";
            case DivF<?> a -> "(" + a.left() + " / " + a.right() + ")";
            case SumF<?> s ->
                    "(" + String.join(" + ",
                        ((List<Integer>) s.values())
                            .stream().map(Object::toString).toList()
                    ) + ")";
        };
    }
}
```

### Highlights

- The backend is replaceable.
- No changes needed in the AST.
- The same DSL can target SQL, JSON, or bytecode-like IR.

## 12.5 Example: Full Pipeline

```java
var expr =
    add(const_(1), const_(2))
        .flatMap(x -> mul(const_(x), const_(3)));

System.out.println(ExprInterpreter.eval(expr));
System.out.println(ExprCodegen.toSource(expr));
System.out.println(
    ExprInterpreter.eval(
        ExprOptimizer.optimize(expr)
    )
);
```

### Output

```text
9
((1 + 2) * 3)
9
```

## 12.6 Summary

This chapter demonstrated the full lifecycle of a Free-based DSL:

| Phase           | Purpose                     |
| --------------- | --------------------------- |
| Interpreter     | Evaluate the expression     |
| Optimization    | Simplify the AST            |
| Code generation | Emit textual representation |

The key takeaway:

> **Free allows the DSL’s structure and meaning to evolve independently.**

This pattern appears in real compilers, database engines, IR pipelines, and DSL interpreters.

## Note on Java’s Type System

The implementation of Free in this chapter necessarily uses `@SuppressWarnings({"rawtypes", "unchecked"})` in several places.
This is not a flaw in Free itself, but a direct consequence of a fundamental limitation in Java’s type system:

- Java does not support _higher-kinded types (HKT)_
- therefore, types such as `Free<F, A>` cannot preserve the shape of `A` inside nested combinators like `flatMap`
- Java erases the essential type information that Scala or Haskell retain

Free monads heavily rely on HKTs to express nested computations with accurate typing.
In Java, we must sacrifice some type precision and manually maintain invariants using casts.

This chapter keeps the implementation correct and safe, but at the cost of noise.

Free, Interpreter, Optimizer, and Codegen can be implemented in Java,
but not without noise: explicit casts, erased type parameters, and
suppressed warnings are unavoidable due to the absence of higher-kinded types.

One may choose to ignore this noise and continue in Java for ecosystem
stability, or embrace a language like Scala where these constructs are
natural and fully type-safe.
The choice depends on whether the goal leans more toward _engineering pragmatism or language expressiveness_.
Both paths are valid — they simply serve different purposes.
