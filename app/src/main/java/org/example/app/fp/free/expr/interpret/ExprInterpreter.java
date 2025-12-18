package org.example.app.fp.free.expr.interpret;

import java.util.function.Function;

import org.example.app.fp.free.Free;
import org.example.app.fp.free.expr.AddF;
import org.example.app.fp.free.expr.ConstF;
import org.example.app.fp.free.expr.DivF;
import org.example.app.fp.free.expr.ExprF;
import org.example.app.fp.free.expr.MulF;
import org.example.app.fp.free.expr.SubF;
import org.example.app.fp.free.expr.SumF;

/**
 * Interpreter for Free<ExprF, Integer> programs.
 * This interpreter evaluates arithmetic expressions represented
 * by the ExprF functor and sequenced by the Free monad.
 *
 * The interpreter reduces the Free structure step-by-step (right-associated
 * normal form) and evaluates each ExprF instruction when reaching Suspend.
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public final class ExprInterpreter {

  /**
   * Evaluate an entire Free<ExprF, Integer> program.
   */
  public static int run(Free<ExprF, Integer> program) {
    Free<ExprF, Integer> curr = program.step();

    while (true) {
      if (curr instanceof Free.Pure<ExprF, Integer> p) {
        return p.value();
      }

      if (curr instanceof Free.Suspend<ExprF, Integer> s) {
        return evalF(s.fa());
      }

      if (curr instanceof Free.FlatMapped<ExprF, ?, Integer> fm) {
        Free<ExprF, ?> sub = fm.sub().step();

        if (sub instanceof Free.Pure<ExprF, ?> sp) {
          Function<Object, Free<ExprF, Integer>> k = unsafeCast(fm.k());
          curr = k.apply(sp.value()).step();
          continue;
        }

        if (sub instanceof Free.Suspend<ExprF, ?> ss) {
          int v = evalF((ExprF<Integer>) ss.fa());
          Function<Object, Free<ExprF, Integer>> k = unsafeCast(fm.k());
          curr = k.apply(v).step();
          continue;
        }

        throw new IllegalStateException("Unexpected Free structure inside FlatMapped");
      }

      throw new IllegalStateException("Unexpected Free structure: " + curr.getClass());
    }
  }

  /**
   * Evaluate one instruction (ExprF node) once its children have concrete integer
   * values.
   */
  private static int evalF(ExprF<Integer> instr) {
    return switch (instr) {
      case ConstF<Integer> c -> c.value();
      case AddF<Integer> a -> a.left() + a.right();
      case SubF<Integer> s -> s.left() - s.right();
      case MulF<Integer> m -> m.left() * m.right();
      case DivF<Integer> d -> d.left() / d.right();
      case SumF<Integer> s -> s.args().stream().mapToInt(Integer::intValue).sum();
      default -> throw new IllegalStateException("Unhandled ExprF case: " + instr.getClass());
    };
  }

  private static <T> T unsafeCast(Object o) {
    return (T) o;
  }
}
