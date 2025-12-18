# Chapter 09 — Building a Minimal Parser Library

In Chapter 8, we introduced the core idea of parser combinators and demonstrated
that Java 21 is expressive enough to encode functional parsing patterns:
`map`, `flatMap`, `or`, structured errors, and chained combinators.

In this chapter, we turn those building blocks into a _small but practical_
parser library. The goal is not to provide a complete parsing framework
(Parsec, Megaparsec, Cats-parse, etc.) but to show how a carefully chosen set of
primitives scales into a usable “mini-language” for writing parsers in a purely
functional style.

## 9.1 Goals of This Minimal Library

A practical parser library must provide:

1. **Compositionality**  
   Complex parsers are built from smaller ones entirely through algebraic
   operators such as `map`, `flatMap`, `or`, and repetition.

2. **Structured errors**  
   End users should see _where_ and _why_ a parse failed.  
   Our `Parser.Error<S>` type already encodes this.

3. **Controlled backtracking**  
   Some grammars are ambiguous, and naive backtracking leads to exponential
   blow-ups or surprising behaviour.  
   Our `fatal` flag (and the `commit()` combinator) gives users fine-grained
   control.

4. **Deterministic semantics**  
   Every combinator must obey the “no consumption on failure” rule.
   This guarantees referential transparency and reliable debugging.

5. **Readable grammar definitions**  
   A parser library should allow grammars to be written declaratively, almost as
   if writing the grammar itself.

The `Parser` and `StringParser` modules we built earlier already satisfy these
requirements; we now organise them into a cohesive whole.

## 9.2 Finalising the Core Types

Every parser has type:

```java
(E, S) -> Either<Error<S>, Result<A,S>>
```

- **E** — environment (immutable input or configuration)
- **S** — state (cursor/index)
- **A** — value

The standard FP interface emerges naturally:

### Functor

```java
map : (A -> B) -> Parser<E,S,A> -> Parser<E,S,B>
```

### Monad

```java
flatMap : (A -> Parser<E,S,B>) -> Parser<E,S,B>
```

### Alternative

```java
or : Parser<E,S,A> -> Parser<E,S,A> -> Parser<E,S,A>
```

### Committed parsing

```java
commit() : promote all failures to fatal
```

This small vocabulary is enough to express a surprisingly rich parsing DSL.

## 9.3 Why `fatal` Is Necessary in Real Grammars

Basic toy parsers can rely on implicit backtracking. But real grammars quickly
run into ambiguity. Consider:

```text
identifier  ::= letter (letter | digit)*
integer     ::= digit+
```

Given input:

```text
123abc
```

Should this be parsed as:

- `integer = 123` followed by `identifier = abc`  
  or
- `identifier = 123abc`?

Different languages choose different resolutions.

If we implement both branches with naive backtracking:

```java
integer().or(identifier())
```

the `integer()` parser will consume `1`, `2`, `3`, then fail at `a`.  
A naive combinator would then rewind the state and try `identifier()`.  
However, for many languages this behaviour is **wrong**.

To express _“once I’ve committed to integer, do not backtrack into identifier”_,
we wrap it in:

```java
integer().commit().or(identifier())
```

Thus `fatal` is a declarative signal:  
**“This path must not be undone.”**

It upgrades a benign failure into a committed failure.

## 9.4 Standard Combinators

`StringParser` provides a standard suite of combinators:

- **Primitives**  
  `chr`, `ch`, `string`, `digits`, `spaces`

- **Repetition**  
  `many`, `many1`

- **Optional**  
  `optional(p, default)`

- **Delimited**  
  `between(left, p, right)`

- **Separated lists**  
  `sepBy(p, sep)`

- **Expression helpers**  
  `chainl1(p, op)` — left-associative operator chains

These mirror combinators in mature libraries such as Parsec/Megaparsec.

## 9.5 Worked Example: Arithmetic Expressions

We now build a parser for arithmetic expressions:

```text
Expr ::= Term (("+" | "-") Term)*
Term ::= Factor (("*" | "/") Factor)*
Factor ::= integer | "(" Expr ")"
```

### Lexical helpers

```java
static final Parser<String,Integer,Character> plus =
    StringParser.ch('+').map(_c -> (BiFunction<Integer,Integer,Integer>) (x,y) -> x + y);

static final Parser<String,Integer,Character> minus =
    StringParser.ch('-').map(_c -> (BiFunction<Integer,Integer,Integer>) (x,y) -> x - y);

...
```

### Recursive parser definition

Because Java lambdas are not self-referential, we wrap recursion in a supplier:

```java
Supplier<Parser<String,Integer,Integer>> exprRef = new Supplier<>() {
  @Override
  public Parser<String,Integer,Integer> get() { return expr; }
};

Parser<String,Integer,Integer> factor =
    StringParser.integer()
      .or(StringParser.between(StringParser.ch('('), () -> exprRef.get(), StringParser.ch(')')));

Parser<String,Integer,Integer> term =
    StringParser.chainl1(factor, mulOp.or(divOp));

Parser<String,Integer,Integer> expr =
    StringParser.chainl1(term, plusOp.or(minusOp));
```

This demonstrates:

- **recursion**
- **operator precedence**
- **left associativity**
- **use of `chainl1`**

### Running the parser

```java
var r = expr.parse("1 + 2 * (3 + 4)", 0);
System.out.println(r);
```

Produces:

```java
Right(Result(15, 13))
```

## 9.6 Error Messages and UX

All parsers eventually produce:

```java
Either<Error<S>, Result<A,S>>
```

Errors include:

- human-readable message
- unexpected token
- expected token
- position
- whether the error is fatal

A user can pretty print via:

```java
err.pretty(Object::toString)
```

Labelled errors help:

```java
expr.label(i -> "at " + i, "expression")
```

This produces:

```java
expected expression at 5 but expected digit...
```

## 9.7 Summary — A Practical Parsing Kernel in Java

In this chapter we transformed a low-level `Parser` abstraction into a coherent
miniature library capable of expressing:

- recursion
- sequences and alternatives
- lexical tokenisation
- committed parsing
- operator-precedence grammars

Java 21’s enhanced pattern matching, records, and sealed types significantly
improve readability and safety. Although not as concise as Scala or Haskell,
Java 21 can host a _clean, principled_ parser combinator framework suitable for
DSLs, interpreters, and educational tools.

This minimal library also prepares the ground for Chapter 10, where we connect
parsing to **Free monads**, **AST construction**, and **interpreter design**.
