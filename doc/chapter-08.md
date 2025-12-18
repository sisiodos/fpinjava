# Chapter 08 — Parser Combinator Basics in Java

Parser combinators are a powerful demonstration of how far we can go with "just functions" and immutable data.
In this chapter we build the conceptual foundation: what a parser is, how it becomes a _combinator_, and why Java 21 is capable of expressing this style without external tools.

This prepares us for the next chapter, where we turn these ideas into a small but usable parsing library.

## 8.1 Why parser combinators?

Traditionally, parsers in Java are built using:

- ad-hoc `String` manipulation,
- regular expressions,
- parser generators (ANTLR, JavaCC, etc.).

These approaches have drawbacks:

- logic is often _implicit_ (buried in regexes or grammar files),
- refactoring requires tool support,
- compositionality is weak.

Parser combinators take a different route:

- a **parser is just a function**, wrapped in a convenient type,
- small parsers are combined into larger parsers,
- the resulting parser is an ordinary Java value.

This fits the overall theme of this book:

> **Treat behaviour as data. Compose it explicitly. Execute at the boundary.**

## 8.2 The core parser type

Conceptually, a parser for values of type `A` is a function:

```text
Input → Either[Error, (A, Input)]
```

In our Java code, we model this more concretely as a parser over `String` plus an index:

```java
// Informal shape (the concrete type lives in fp.combinator.Parser):
// (String input, int position) -> Either<ParseError, Result<A>>
```

Where:

- `Result<A>` holds the parsed value and the next input position,
- `ParseError` describes what went wrong (and later, whether it is recoverable),
- `Either` is our FP-style sum type from earlier chapters.

The `Parser<A>` type simply wraps this function together with combinators such as `map`, `flatMap`, and `orElse`.

The essential idea is:

- a parser consumes a prefix of the input,
- returns a value plus the remaining suffix,
- or fails with a well-typed error.

## 8.3 Primitive parsers

Before we can compose parsers, we need a few primitive building blocks. Typical examples are:

- `char(c)` — parse a single expected character,
- `digit()` — parse a decimal digit,
- `string(s)` — parse a fixed string,
- `eof()` — succeed only at the end of input.

In our Java implementation, these are exposed as static factory methods on `Parser` or helper classes such as `StringParser`.

A schematic example for `char(c)` looks like this:

```java
public static Parser<Character> ch(char expected) {
  return (input, pos) -> {
    if (pos < input.length() && input.charAt(pos) == expected) {
      return Either.right(new Result<>(expected, pos + 1));
    } else {
      return Either.left(ParseError.unexpectedChar(input, pos, expected));
    }
  };
}
```

This already exhibits the key pattern:

- read from `(input, pos)`,
- on success, advance `pos`,
- on failure, return a structured error without throwing.

## 8.4 From primitive parsers to combinators

A single primitive parser is not very interesting. The power comes from _combinators_—higher-order functions that take parsers as arguments and return new parsers.

The two fundamental ones are familiar by now:

- `map` — transform the parsed value,
- `flatMap` — sequence parsers where the second depends on the first.

Conceptually:

```java
Parser<A> p;
Function<A, B> f;
Parser<B> q = p.map(f);

Function<A, Parser<B>> k;
Parser<B> r = p.flatMap(k);
```

With these in place, we can write code such as:

```java
Parser<Integer> twoDigits =
    digit().flatMap(d1 ->
    digit().map(d2 ->
        10 * d1 + d2
    ));
```

This is structurally similar to the `State` and `Io` monads:

- _input_ plays the role of the state,
- the parser’s result carries both a value and the new state (input position),
- `flatMap` threads this state automatically.

In other words:

> **Parser is a specialised state monad over input streams.**

## 8.5 Choice and repetition

To be useful, parsers must be able to express:

- alternatives ("try this parser, or that one"),
- repetition ("zero or more", "one or more").

We encode these as combinators as well.

### Choice

Choice tries the first parser; if it fails _without consuming input_, it tries the second:

```java
Parser<A> or(Parser<A> p, Parser<A> q);
```

This allows expressions like:

```java
Parser<Character> sign =
    ch('+').orElse(ch('-'));
```

Later, we will refine this with the notion of _recoverable_ vs _fatal_ errors, and a `commit` combinator that fixes the branch. For now, we stay with the simpler, recoverable form of choice.

### Repetition

Repetition combinators build on `flatMap` and choice:

- `many(p)` — zero or more repetitions of `p`,
- `many1(p)` — one or more repetitions of `p`.

Their implementation follows the typical pattern:

```java
public static <A> Parser<java.util.List<A>> many(Parser<A> p) {
  return inputStart -> {
    var out = new java.util.ArrayList<A>();
    var input = inputStart;

    while (true) {
      var result = p.parse(input);
      if (result.isRight()) {
        var r = result.getRightUnsafe();
        out.add(r.value());
        input = r.next();
      } else {
        // on the first failure, stop and succeed with what we have
        return Either.right(new Result<>(out, input));
      }
    }
  };
}
```

The exact signature in our code is slightly different (we separate `input` and `position`), but the idea is identical.

## 8.6 How this feels in Java 21

Compared to FP-first languages, parser combinators in Java still have some syntactic overhead:

- more explicit types,
- more boilerplate around `Either` and `Result`,
- less concise lambda syntax.

However, Java 21 gives us several advantages that earlier Java versions lacked:

- records make `Result` and `ParseError` lightweight and immutable,
- sealed types allow `ParseError` and ASTs to be closed and exhaustively matched,
- pattern matching in `switch` improves readability of error handling.

Crucially, **the core ideas survive translation almost unchanged**:

- parsers are pure values,
- combinators compose them into larger grammars,
- errors are data, not exceptions.

This chapter has focused on the _shape_ of parser combinators.  
In the next chapter, we will organise these ideas into a minimal reusable library, and address more advanced concerns such as fatal errors, backtracking control, and practical string parsing utilities.
