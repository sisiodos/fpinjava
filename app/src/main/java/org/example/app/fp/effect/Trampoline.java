package org.example.app.fp.effect;

import java.util.function.Function;
import java.util.function.Supplier;

// 最小のTrampoline実装 必要になったら差し込む
// 深い再起だけトランポリン化するイメージ
// static Trampoline<Long> factT(long n, long acc) {
//  return n == 0 ? Trampoline.done(acc)
//                : Trampoline.suspend(() -> factT(n - 1, acc * n));
// }
// factT(100_000, 1).run(); // StackOverflowしない
public sealed interface Trampoline<A> permits Trampoline.Done, Trampoline.Cont {
  record Done<A>(A value) implements Trampoline<A> {}
  record Cont<A>(Supplier<Trampoline<A>> next) implements Trampoline<A> {}

  static <A> Trampoline<A> done(A a) { return new Done<>(a); }
  static <A> Trampoline<A> suspend(Supplier<Trampoline<A>> k) { return new Cont<>(k); }

  default A run() {
    Trampoline<A> t = this;
    while (true) {
      if (t instanceof Done<A> d) return d.value();
      t = ((Cont<A>) t).next().get();
    }
  }

  default <B> Trampoline<B> map(Function<? super A, ? extends B> f) {
    return flatMap(a -> done(f.apply(a)));
  }

  default <B> Trampoline<B> flatMap(Function<? super A, Trampoline<B>> f) {
    return switch (this) {
      case Done<A> d -> f.apply(d.value());
      case Cont<A> c -> suspend(() -> c.next().get().flatMap(f));
    };
  }
}