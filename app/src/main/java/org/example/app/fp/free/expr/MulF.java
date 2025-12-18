package org.example.app.fp.free.expr;

import java.util.function.Function;

public record MulF<A>(A left, A right) implements ExprF<A> {
  @Override
  public <B> ExprF<B> map(Function<A, B> f) {
    return new MulF<>(f.apply(left), f.apply(right));
  }
}
