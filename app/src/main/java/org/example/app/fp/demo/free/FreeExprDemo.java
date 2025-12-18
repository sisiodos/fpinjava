package org.example.app.fp.demo.free;

import org.example.app.fp.free.Free;
import org.example.app.fp.free.expr.*;
import org.example.app.fp.free.expr.interpret.ExprInterpreter;

/**
 * Demonstration of constructing arithmetic expressions using
 * Free<ExprF, Integer> and evaluating them with ExprInterpreter.
 */
@SuppressWarnings({ "rawtypes" })
public final class FreeExprDemo {

  public static void main(String[] args) {

    // ------------------------------------------------------------
    // 1. (10 + 20) * (3 - 1)
    // ------------------------------------------------------------
    Free<ExprF, Integer> expr1 = mul(
        add(lit(10), lit(20)),
        sub(lit(3), lit(1)));

    System.out.println("Expr1 AST = " + expr1);
    int result1 = ExprInterpreter.run(expr1);
    System.out.println("Expr1 result = " + result1); // 60

    // ------------------------------------------------------------
    // 2. (5 + 7) / 3
    // ------------------------------------------------------------
    Free<ExprF, Integer> expr2 = div(
        add(lit(5), lit(7)),
        lit(3));

    System.out.println("Expr2 AST = " + expr2);
    int result2 = ExprInterpreter.run(expr2);
    System.out.println("Expr2 result = " + result2); // 4

    // ------------------------------------------------------------
    // 3. SumF example: sum(1,2,3,4,5)
    // ------------------------------------------------------------
    Free<ExprF, Integer> expr3 = sum(lit(1), lit(2), lit(3), lit(4), lit(5));

    System.out.println("Expr3 AST = " + expr3);
    int result3 = ExprInterpreter.run(expr3);
    System.out.println("Expr3 result = " + result3); // 15

    // ------------------------------------------------------------
    // 4. Deeply nested expression with automatic normalization
    // ((1+2)+(3+4))+(5+6)
    // ------------------------------------------------------------
    Free<ExprF, Integer> nested = add(
        add(add(lit(1), lit(2)), add(lit(3), lit(4))),
        add(lit(5), lit(6)));

    System.out.println("Nested AST = " + nested);
    int nestedResult = ExprInterpreter.run(nested);
    System.out.println("Nested result = " + nestedResult); // 21
  }

  // ==================================================================
  // Convenience constructors — these wrap ExprF nodes in Free.liftF
  // ==================================================================

  private static Free<ExprF, Integer> lit(int value) {
    return Free.liftF(new ConstF<>(value));
  }

  private static Free<ExprF, Integer> add(Free<ExprF, Integer> a, Free<ExprF, Integer> b) {
    return Free.liftF(new AddF<>(a, b));
  }

  private static Free<ExprF, Integer> sub(Free<ExprF, Integer> a, Free<ExprF, Integer> b) {
    return Free.liftF(new SubF<>(a, b));
  }

  private static Free<ExprF, Integer> mul(Free<ExprF, Integer> a, Free<ExprF, Integer> b) {
    return Free.liftF(new MulF<>(a, b));
  }

  private static Free<ExprF, Integer> div(Free<ExprF, Integer> a, Free<ExprF, Integer> b) {
    return Free.liftF(new DivF<>(a, b));
  }

  @SafeVarargs
  private static Free<ExprF, Integer> sum(Free<ExprF, Integer>... args) {
    return Free.liftF(new SumF<>(java.util.List.of(args)));
  }
}