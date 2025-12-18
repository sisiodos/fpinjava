package org.example.app.fp.free.expr;

import java.util.function.Function;

public record ConstF<A>(int value) implements ExprF<A> {
  @Override
  public <B> ExprF<B> map(Function<A, B> f) {
    return new ConstF<>(value);
  }
}
