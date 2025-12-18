package org.example.app.fp.free.expr;

import java.util.List;
import java.util.function.Function;

public record SumF<A>(List<A> args) implements ExprF<A> {
  @Override
  public <B> ExprF<B> map(Function<A, B> f) {
    return new SumF<>(args.stream().map(f).toList());
  }
}
