package org.example.app.fp.free.expr;

import java.util.function.Function;

public sealed interface ExprF<A> permits ConstF, AddF, MulF, SubF, DivF, SumF {
  <B> ExprF<B> map(Function<A, B> f);
}
