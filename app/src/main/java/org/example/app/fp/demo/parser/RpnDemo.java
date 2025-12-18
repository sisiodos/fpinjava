package org.example.app.fp.demo.parser;

import org.example.app.fp.combinator.Parser;
import org.example.app.fp.combinator.StringParser;
import org.example.app.fp.core.Either;

import java.util.ArrayDeque;
import java.util.List;
import java.util.function.BiFunction;

public class RpnDemo {

  /* ===== RPN Token ADT ===== */
  sealed interface RpnTok permits Num, Op {}
  record Num(int value) implements RpnTok {}
  record Op(String symbol, BiFunction<Integer,Integer,Integer> f) implements RpnTok {}

  // Numeric token: "123" -> Num(123)  (trailing whitespace consumed by token)
  static Parser<String, Integer, RpnTok> numberTok() {
    return StringParser.token(StringParser.integer().map(Num::new));
  }

  // Operator tokens: + - * / mapped to Op
  static Parser<String, Integer, RpnTok> opTok() {
    Parser<String, Integer, RpnTok> plus  = StringParser.token(StringParser.ch('+')
        .map(_c -> new Op("+", (a,b) -> a + b)));
    Parser<String, Integer, RpnTok> minus = StringParser.token(StringParser.ch('-')
        .map(_c -> new Op("-", (a,b) -> a - b)));
    Parser<String, Integer, RpnTok> times = StringParser.token(StringParser.ch('*')
        .map(_c -> new Op("*", (a,b) -> a * b)));
    Parser<String, Integer, RpnTok> div   = StringParser.token(StringParser.ch('/')
        .map(_c -> new Op("/", (a,b) -> {
          if (b == 0) throw new ArithmeticException("division by zero");
          return a / b;
        })));
    return plus.or(minus).or(times).or(div);
  }

  // One token (number or operator)
  static Parser<String, Integer, RpnTok> token() {
    return numberTok().or(opTok());
  }

  // Entire RPN input: one or more tokens
  static Parser<String, Integer, List<RpnTok>> tokens() {
    return StringParser.many1(token());
  }

  /* ===== Stack evaluator ===== */

  static Either<String, Integer> eval(List<RpnTok> toks) {
    var st = new ArrayDeque<Integer>();
    try {
      for (RpnTok t : toks) {
        if (t instanceof Num n) {
          st.push(n.value());
        } else if (t instanceof Op op) {
          if (st.size() < 2) return Either.left("stack underflow at '" + op.symbol() + "'");
          int b = st.pop();
          int a = st.pop();
          st.push(op.f().apply(a, b));
        }
      }
      if (st.size() != 1) return Either.left("remaining stack: " + st);
      return Either.right(st.pop());
    } catch (ArithmeticException ex) {
      return Either.left(ex.getMessage());
    }
  }

  /* ===== Demo usage ===== */

  public static void main(String[] args) {
    run("3 4 +");             // => 7
    run("2 3 4 + *");         // => 14
    run("5 1 2 + 4 * + 3 -"); // => 14
    run("10 0 /");            // => error: division by zero
    run("+");                 // => error: stack underflow
  }

  static void run(String src) {
    var parsed = tokens().parse(src, 0);
    parsed.fold(
        err -> { System.out.println("parse error @"+err.message()); return null; },
        ok  -> {
          var valueOrErr = eval(ok.value());
          valueOrErr.fold(
              em -> { System.out.println("eval error: "+em); return null; },
              v  -> { System.out.println(src + " = " + v); return null; }
          );
          return null;
        }
    );
  }
}