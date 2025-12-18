package org.example.app.fp.free.expr;

import java.util.function.Function;

public record SubF<A>(A left, A right) implements ExprF<A> {
  @Override
  public <B> ExprF<B> map(Function<A, B> f) {
    return new SubF<>(f.apply(left), f.apply(right));
  }
}
