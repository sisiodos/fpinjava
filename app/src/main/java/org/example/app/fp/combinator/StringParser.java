package org.example.app.fp.combinator;

import org.example.app.fp.core.Either;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Predicate;

/**
 * StringParser — parser primitives specialized for String input and Integer
 * position.
 *
 * <p>
 * All parsers here have the shape: {@code Parser<String, Integer, A>}.
 * Failures are reported as {@code Either.left(Parser.Error<Integer>)}
 * with {@code fatal=false} by default.
 *
 * <p>
 * <b>Usage example:</b>
 * <pre>{@code
 * Parser<String, Integer, Integer> parser = StringParser.integer();
 * var result = parser.parse("12345 xyz", 0);
 * result.fold(
 *     err -> { System.out.println("Error: " + err.message()); return null; },
 *     ok -> { System.out.println("Value: " + ok.value()); return null; }
 * );
 * }</pre>
 * <p>
 * Each static method (e.g. {@code digits()}, {@code string()}, {@code integer()})
 * acts as a parser factory for common token patterns in string input.
 */
public final class StringParser {
  private StringParser() {
  }

  // --- helpers --------------------------------------------------------------
  private static Parser.Error<Integer> err(String message, int pos, String expected) {
    return err(message, pos, expected, false);
  }

  private static Parser.Error<Integer> err(String message, int pos, String expected, boolean fatal) {
    return new Parser.Error<>(message, pos, expected, fatal);
  }

  private static <L, R> R getRightUnsafe(Either<L, R> e) {
    return e.fold(l -> {
      throw new IllegalStateException("Expected Right but got Left: " + l);
    }, r -> r);
  }

  // --- primitives -----------------------------------------------------------

  /**
   * Parser for a single character that satisfies the given predicate.
   *
   * <p>On success: consumes one character and returns it.
   * On failure: does not consume input and returns {@code Error<Integer>} with {@code expected = expect}.
   *
   * <pre>{@code
   * // a lowercase letter
   * var lower = StringParser.chr(Character::isLowerCase, "lowercase");
   * lower.parse("abc", 0)  // => Right(Result('a', 1))
   * lower.parse("123", 0)  // => Left(Error(pos=0, expected="lowercase"))
   * }</pre>
   *
   * @param p predicate to test the next character
   * @param expect description used in error messages (e.g., "digit")
   */
  public static Parser<String, Integer, Character> chr(Predicate<Character> p, String expect) {
    return (in, i) -> {
      if (i >= in.length()) {
        return Either.left(err("end-of-input", i, expect));
      }
      char c = in.charAt(i);
      return p.test(c)
          ? Either.right(new Parser.Result<>(c, i + 1))
          : Either.left(err("expected " + expect + " got '" + c + "'", i, expect));
    };
  }

  /**
   * Parser for an exact character literal.
   *
   * <p>Equivalent to {@code chr(x -> x == c, "'" + c + "'")}.
   *
   * <pre>{@code
   * var open = StringParser.ch('(');
   * open.parse("(x", 0)   // => Right(Result('(', 1))
   * open.parse("x", 0)    // => Left(Error(pos=0, expected="'('"))
   * }</pre>
   *
   * @param c the character to match
   */
  public static Parser<String, Integer, Character> ch(char c) {
    return chr(x -> x == c, "'" + c + "'");
  }

  /**
   * Parser for an exact string literal.
   *
   * <p>On success: consumes {@code lit.length()} characters and returns {@code lit}.
   * On failure: does not consume input and reports {@code expected="\"lit\""}.
   *
   * <pre>{@code
   * var kw = StringParser.string("let");
   * kw.parse("let x", 0)  // => Right(Result("let", 3))
   * kw.parse("lex", 0)    // => Left(Error(pos=0, expected="\"let\""))
   * }</pre>
   *
   * @param lit the literal string to match
   */
  public static Parser<String, Integer, String> string(String lit) {
    return (in, i) -> {
      int end = i + lit.length();
      if (end <= in.length() && in.regionMatches(i, lit, 0, lit.length())) {
        return Either.right(new Parser.Result<>(lit, end));
      }
      return Either.left(err("expected \"" + lit + "\"", i, "\"" + lit + "\""));
    };
  }

  /**
   * Zero-or-more repetition.
   *
   * <p>Applies {@code p} repeatedly until it fails, collecting results in a list.
   * Always succeeds (possibly with an empty list). Fails from {@code p} do not consume input.
   *
   * <pre>{@code
   * var digits = StringParser.many(StringParser.chr(Character::isDigit, "digit"));
   * digits.parse("123a", 0)  // => Right(Result(["1","2","3"], 3))
   * digits.parse("abc", 0)   // => Right(Result([], 0))
   * }</pre>
   *
   * @param p element parser
   * @param <A> element type
   */
  public static <A> Parser<String, Integer, List<A>> many(Parser<String, Integer, A> p) {
    return (in, i0) -> {
      var out = new ArrayList<A>();
      var pos = new int[]{i0};
      while (true) {
        var r = p.parse(in, pos[0]);
        if (r.isRight()) {
          var rr = getRightUnsafe(r);
          out.add(rr.value());
          pos[0] = rr.next();
        } else {
          // If the failure is fatal, propagate it. Otherwise, stop and succeed with what we have.
          return r.fold(
              err -> err.fatal()
                  ? Either.left(err)
                  : Either.right(new Parser.Result<>(out, pos[0])),
              ok -> {
                throw new IllegalStateException("Unreachable: expected Left but got Right");
              });
        }
      }
    };
  }

  /**
   * One-or-more repetition.
   *
   * <p>Like {@link #many(Parser)} but requires at least one successful match.
   *
   * <pre>{@code
   * var ds1 = StringParser.many1(StringParser.chr(Character::isDigit, "digit"));
   * ds1.parse("123a", 0) // => Right(Result(['1','2','3'], 3))
   * ds1.parse("abc", 0)  // => Left(Error(pos=0, expected="digit"))
   * }</pre>
   *
   * @param p element parser
   * @param <A> element type
   */
  public static <A> Parser<String, Integer, List<A>> many1(Parser<String, Integer, A> p) {
    return p.flatMap(head -> many(p).map(tail -> {
      var out = new ArrayList<A>(tail.size() + 1);
      out.add(head);
      out.addAll(tail);
      return out;
    }));
  }

  /**
   * Optional combinator with default value.
   *
   * <p>Tries {@code p}. If it succeeds, returns its value; if it fails, succeeds without consuming input
   * and returns {@code defaultValue}.
   *
   * <pre>{@code
   * var sign = StringParser.optional(StringParser.ch('-').map(_c -> -1), 1);
   * sign.parse("-12", 0) // => Right(Result(-1, 1))
   * sign.parse("12", 0)  // => Right(Result(1, 0))
   * }</pre>
   *
   * @param p parser to try
   * @param defaultValue value to return on failure
   * @param <A> result type
   */
  public static <A> Parser<String, Integer, A> optional(Parser<String, Integer, A> p, A defaultValue) {
    return (in, i) -> {
      var r = p.parse(in, i);
      if (r.isRight()) {
        return r;
      }
      return r.fold(
          err -> err.fatal()
              ? Either.left(err)
              : Either.right(new Parser.Result<>(defaultValue, i)),
          ok -> {
            throw new IllegalStateException("Unreachable: expected Left but got Right");
          });
    };
  }

  /**
   * Wraps a parser with left and right delimiters.
   *
   * <p>Parses {@code left}, then {@code p}, then {@code right}; returns the middle value.
   *
   * <pre>{@code
   * var parens = StringParser.between(StringParser.ch('('), StringParser.digits(), StringParser.ch(')'));
   * parens.parse("(123)", 0) // => Right(Result("123", 5))
   * }</pre>
   */
  public static <A, L, R> Parser<String, Integer, A> between(
      Parser<String, Integer, L> left,
      Parser<String, Integer, A> p,
      Parser<String, Integer, R> right) {
    return left.flatMap(_l -> p.flatMap(v -> right.map(_r -> v)));
  }

  /**
   * Zero-or-more elements separated by a separator parser.
   *
   * <p>Parses {@code p (sep p)*}. Succeeds with an empty list if the first {@code p} fails.
   *
   * <pre>{@code
   * var comma = StringParser.ch(',');
   * var ints = StringParser.sepBy(StringParser.integer(), comma);
   * ints.parse("1,2,3", 0) // => Right(Result([1,2,3], 5))
   * ints.parse("", 0)      // => Right(Result([], 0))
   * }</pre>
   *
   * @param p element parser
   * @param sep separator parser
   */
  public static <A, S> Parser<String, Integer, List<A>> sepBy(
      Parser<String, Integer, A> p,
      Parser<String, Integer, S> sep) {
    return optional(
        p.flatMap(first -> many(sep.flatMap(_s -> p)).map(rest -> {
          var out = new ArrayList<A>(rest.size() + 1);
          out.add(first);
          out.addAll(rest);
          return out;
        })),
        List.<A>of()).label(pos -> String.valueOf(pos), "sepBy");
  }

  /**
   * Parses and returns a (possibly empty) sequence of whitespace characters.
   *
   * <p>Always succeeds; returns the matched whitespace as a string.
   */
  public static Parser<String, Integer, String> spaces() {
    return many(chr(Character::isWhitespace, "whitespace")).map(cs -> {
      var sb = new StringBuilder();
      cs.forEach(sb::append);
      return sb.toString();
    });
  }

  /**
   * Tokenizes a parser by consuming trailing whitespace.
   *
   * <p>Applies {@code p} and then {@link #spaces()}, returning the value of {@code p}.
   * Useful for lexeme-level parsers.
   */
  public static <A> Parser<String, Integer, A> token(Parser<String, Integer, A> p) {
    return p.flatMap(v -> spaces().map(_sp -> v));
  }

  /**
   * One-or-more decimal digits as a string.
   *
   * <p>Equivalent to {@code many1(chr(isDigit, "digit")).map(chars -> ...)}.
   */
  public static Parser<String, Integer, String> digits() {
    return many1(chr(Character::isDigit, "digit")).map(cs -> {
      var sb = new StringBuilder();
      cs.forEach(sb::append);
      return sb.toString();
    });
  }

  /**
   * Signed integer token: optional '-' followed by one-or-more digits, then trailing spaces.
   *
   * <p>Returns the parsed {@code Integer} value.
   *
   * <pre>{@code
   * var intTok = StringParser.integer();
   * intTok.parse("-42  ", 0) // => Right(Result(-42, 4))
   * }</pre>
   */
  public static Parser<String, Integer, Integer> integer() {
    var sign = optional(ch('-').map(_c -> -1), 1);
    return token(sign.flatMap(s -> digits().map(ds -> s * Integer.parseInt(ds))))
        .label(pos -> String.valueOf(pos), "integer");
  }

  /**
   * Left-associative chain: parses {@code a (op a)*} and folds left with the provided operator.
   *
   * <p>Commonly used for expressions with left-associative binary operators (e.g., addition).
   *
   * <pre>{@code
   * var plus = StringParser.ch('+').map(_c -> (BiFunction<Integer,Integer,Integer>)(x,y) -> x + y);
   * var expr = StringParser.chainl1(StringParser.integer(), plus);
   * expr.parse("1+2+3", 0) // => Right(Result(6, 5))
   * }</pre>
   *
   * @param p the atomic term parser
   * @param op binary operator parser producing {@code BiFunction<A,A,A>}
   * @param <A> result type
   */
  public static <A> Parser<String, Integer, A> chainl1(
      Parser<String, Integer, A> p,
      Parser<String, Integer, BiFunction<A, A, A>> op) {
    return p.flatMap(a -> many(op.flatMap(f -> p.map(b -> new Pair<>(f, b))))
        .map(pairs -> {
          A acc = a;
          for (var pr : pairs)
            acc = pr.f.apply(acc, pr.s);
          return acc;
        }));
  }

  /**
   * Simple immutable pair used internally by {@link #chainl1(Parser, Parser)}.
   */
  private static final class Pair<F, S> {
    final F f;
    final S s;

    Pair(F f, S s) {
      this.f = f;
      this.s = s;
    }
  }
}
