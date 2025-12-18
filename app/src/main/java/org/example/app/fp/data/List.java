package org.example.app.fp.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Immutable singly-linked list (FP style).
 * <p>
 * Two cases only: {@link List.Nil} (empty) or {@link List.Cons} (non-empty).
 * All operations are pure and return new lists without mutating existing ones.
 * <p>
 * This structure mirrors Scala's List and is designed for teaching FP in Java.
 */
public sealed interface List<A> permits List.Nil, List.Cons {

  // ------------------------------------------------------------
  // Queries (O(1) for isEmpty/head/tail)
  // ------------------------------------------------------------
  boolean isEmpty();

  A head();

  List<A> tail();

  // ------------------------------------------------------------
  // Constructors / factories
  // ------------------------------------------------------------
  static <A> List<A> empty() {
    return new Nil<>();
  }

  static <A> List<A> cons(A head, List<A> tail) {
    return new Cons<>(head, tail);
  }

  @SafeVarargs
  static <A> List<A> of(A... values) {
    List<A> result = empty();
    for (int i = values.length - 1; i >= 0; i--) {
      result = cons(values[i], result);
    }
    return result;
  }

  static <A> List<A> fromJava(java.util.List<? extends A> src) {
    List<A> result = empty();
    for (int i = src.size() - 1; i >= 0; i--) {
      result = cons(src.get(i), result);
    }
    return result;
  }

  default java.util.List<A> toJavaList() {
    var out = new ArrayList<A>();
    var cur = this;
    while (!cur.isEmpty()) {
      out.add(cur.head());
      cur = cur.tail();
    }
    return Collections.unmodifiableList(out);
  }

  // ------------------------------------------------------------
  // Core folds (structural recursion)
  // ------------------------------------------------------------
  default <B> B foldRight(B z, BiFunction<? super A, ? super B, ? extends B> f) {
    return isEmpty() ? z : f.apply(head(), tail().foldRight(z, f));
  }

  default <B> B foldLeft(B z, BiFunction<? super B, ? super A, ? extends B> f) {
    var cur = this;
    B acc = z;
    while (!cur.isEmpty()) {
      acc = f.apply(acc, cur.head());
      cur = cur.tail();
    }
    return acc;
  }

  // ------------------------------------------------------------
  // Functor / Monad-like combinators
  // ------------------------------------------------------------
  default <B> List<B> map(Function<? super A, ? extends B> f) {
    // Java17 equivalent:
    // return foldRight(
    //   List.<B>empty(),
    //   (a, bs) -> cons(f.apply(a), bs)
    // );
    return switch (this) {
      case Nil() -> List.empty();
      case Cons(var h, var t) -> List.cons(f.apply(h), t.map(f));
    };
  }

  default List<A> filter(Predicate<? super A> p) {
    // Java17 equivalent:
    // return foldRight(
    //   List.<A>empty(),
    //   (a, as) -> p.test(a) ? cons(a, as) : as
    // );
    return switch (this) {
      case Nil() -> List.empty();
      case Cons(var h, var t) ->
          p.test(h) ? List.cons(h, t.filter(p)) : t.filter(p);
    };
  }

  default <B> List<B> flatMap(Function<? super A, ? extends List<B>> f) {
    // Java17 equivalent:
    // return foldRight(
    //   List.<B>empty(),
    //   (a, bs) -> concat(f.apply(a), bs)
    // );
    return switch (this) {
      case Nil() -> List.empty();
      case Cons(var h, var t) -> List.concat(f.apply(h), t.flatMap(f));
    };
  }

  // ------------------------------------------------------------
  // Basic list ops
  // ------------------------------------------------------------
  default List<A> append(List<A> other) {
    // Java17 equivalent:
    // return foldRight(other, List::cons);
    return switch (this) {
      case Nil() -> other;
      case Cons(var h, var t) -> List.cons(h, t.append(other));
    };
  }

  static <A> List<A> concat(List<A> xs, List<A> ys) {
    // Java17 equivalent:
    // return xs.append(ys);
    return switch (xs) {
      case Nil() -> ys;
      case Cons(var h, var t) -> List.cons(h, concat(t, ys));
    };
  }

  static <A> List<A> concat(List<List<A>> lists) {
    // Java17 equivalent:
    // return lists.foldRight(
    //   List.empty(),
    //   List::append
    // );
    return switch (lists) {
      case Nil() -> List.empty();
      case Cons(var h, var t) -> concat(h, concat(t));
    };
  }

  default List<A> reverse() {
    // Java17 equivalent:
    // return foldLeft(
    //   List.<A>empty(),
    //   (acc, a) -> cons(a, acc)
    // );
    return switch (this) {
      case Nil() -> this;
      case Cons(var h, var t) -> concat(t.reverse(), List.cons(h, List.empty()));
    };
  }

  default int length() {
    // Java17 equivalent:
    // return foldLeft(
    //   0,
    //   (n, __) -> n + 1
    // );
    return switch (this) {
      case Nil() -> 0;
      case Cons(var h, var t) -> 1 + t.length();
    };
  }

  default List<A> take(int n) {
    // Java17 equivalent:
    // Objects.checkIndex(Math.max(0, n), Integer.MAX_VALUE); // bounds sanity
    // var cur = this;
    // int k = n;
    // List<A> acc = empty();
    // while (!cur.isEmpty() && k > 0) {
    //   acc = cons(cur.head(), acc);
    //   cur = cur.tail();
    //   k--;
    // }
    // return acc.reverse();
    return switch (this) {
      case Nil() -> this;
      case Cons(var h, var t) -> n <= 0
          ? List.empty()
          : List.cons(h, t.take(n - 1));
    };
  }

  default List<A> drop(int n) {
    // Java17 equivalent:
    // var cur = this;
    // int k = n;
    // while (!cur.isEmpty() && k > 0) {
    //   cur = cur.tail();
    //   k--;
    // }
    // return cur;
    return switch (this) {
      case Nil() -> this;
      case Cons(var h, var t) -> n <= 0 ? this : t.drop(n - 1);
    };
  }

  default List<A> takeWhile(Predicate<? super A> p) {
    // Java17 equivalent:
    // return foldRight(
    //   List.<A>empty(), (a, as) -> p.test(a) ? cons(a, as) : as
    // );
    return switch (this) {
      case Nil() -> this;
      case Cons(var h, var t) ->
          p.test(h) ? List.cons(h, t.takeWhile(p)) : List.empty();
    };
  }

  default boolean exists(Predicate<? super A> p) {
    // Java17 equivalent:
    // var cur = this;
    // while (!cur.isEmpty()) {
    //   if (p.test(cur.head()))
    //     return true;
    //   cur = cur.tail();
    // }
    // return false;
    return switch (this) {
      case Nil() -> false;
      case Cons(var h, var t) -> p.test(h) || t.exists(p);
    };
  }

  default boolean forAll(Predicate<? super A> p) {
    // Java17 equivalent:
    // return !exists(p.negate());
    return switch (this) {
      case Nil() -> true;
      case Cons(var h, var t) -> p.test(h) && t.forAll(p);
    };
  }

  // ------------------------------------------------------------
  // Cases
  // ------------------------------------------------------------
  /** Empty list */
  record Nil<A>() implements List<A> {
    @Override
    public boolean isEmpty() {
      return true;
    }

    @Override
    public A head() {
      throw new UnsupportedOperationException("Nil.head");
    }

    @Override
    public List<A> tail() {
      throw new UnsupportedOperationException("Nil.tail");
    }

    @Override
    public String toString() {
      return "[]";
    }
  }

  /** Non-empty list */
  record Cons<A>(A head, List<A> tail) implements List<A> {
    @Override
    public boolean isEmpty() {
      return false;
    }

    @Override
    public String toString() {
      var sb = new StringBuilder("[");
      var cur = (List<A>) this;
      while (!cur.isEmpty()) {
        sb.append(cur.head());
        cur = cur.tail();
        if (!cur.isEmpty())
          sb.append(',').append(' ');
      }
      return sb.append(']').toString();
    }
  }
}
