package org.example.app.fp.util;

import java.util.ArrayList;
import java.util.function.Function;

import org.example.app.fp.core.Either;
import org.example.app.fp.core.Option;
import org.example.app.fp.core.Try;
import org.example.app.fp.data.Tree;
import org.example.app.fp.data.List;
import org.example.app.fp.effect.Io;

/**
 * Utility methods for traverse and sequence operations in FP style.
 * 
 * This class provides implementations for concrete target types (Option,
 * Either, Try, Io)
 * combined with concrete container types (Tree, java.util.List).
 * 
 * Purpose:
 * - traverse: Traverse the container with a function returning F<A>, lifting to
 * F<Container<B>>
 * - sequence: Aggregate effects from Container<F<A>> to F<Container<A>>
 * 
 * This implementation does not use type class generalization
 * (Applicative/Functor),
 * but optimizes for each type practically.
 */
public final class Traversals {
  private Traversals() {
  }

  // ---------------------------------------------------------------------------
  // Tree × Option / Either / Try / Io
  // ---------------------------------------------------------------------------

  // --- Option ---

  public static <A, B> Option<Tree<B>> traverseOption(Tree<A> t, Function<? super A, Option<B>> f) {
    // Java17 equivalent (conceptual):
    // return t.fold(
    // a -> f.apply(a).map(Tree::leaf),
    // (lo, ro) -> lo.flatMap(l -> ro.map(r -> Tree.branch(l, r))));
    return switch (t) {
      case Tree.Leaf(var v) -> f.apply(v).map(Tree::leaf);
      case Tree.Branch(var l, var r) ->
        traverseOption(l, f).flatMap(lb -> traverseOption(r, f).map(rb -> Tree.branch(lb, rb)));
    };
  }

  public static <A> Option<Tree<A>> sequenceOption(Tree<Option<A>> t) {
    return traverseOption(t, Function.identity());
  }

  // --- Either ---

  public static <L, A, B> Either<L, Tree<B>> traverseEither(Tree<A> t, Function<? super A, Either<L, B>> f) {
    // Java17 equivalent (conceptual):
    // return t.fold(
    // a -> f.apply(a).map(Tree::leaf),
    // (lo, ro) -> lo.flatMap(l -> ro.map(r -> Tree.branch(l, r))));
    return switch (t) {
      case Tree.Leaf(var v) -> f.apply(v).map(Tree::leaf);
      case Tree.Branch(var l, var r) ->
        traverseEither(l, f).flatMap(lb -> traverseEither(r, f).map(rb -> Tree.branch(lb, rb)));
    };
  }

  public static <L, A> Either<L, Tree<A>> sequenceEither(Tree<Either<L, A>> t) {
    return traverseEither(t, Function.identity());
  }

  // --- Try ---

  public static <A, B> Try<Tree<B>> traverseTry(Tree<A> t, Function<? super A, Try<B>> f) {
    // Java17 equivalent (conceptual):
    // return t.fold(
    // a -> f.apply(a).map(Tree::leaf),
    // (lt, rt) -> lt.flatMap(l -> rt.map(r -> Tree.branch(l, r))));
    return switch (t) {
      case Tree.Leaf(var v) -> f.apply(v).map(Tree::leaf);
      case Tree.Branch(var l, var r) ->
        traverseTry(l, f).flatMap(lb -> traverseTry(r, f).map(rb -> Tree.branch(lb, rb)));
    };
  }

  public static <A> Try<Tree<A>> sequenceTry(Tree<Try<A>> t) {
    return traverseTry(t, Function.identity());
  }

  // --- Io ---

  public static <A, B> Io<Tree<B>> traverseIo(Tree<A> t, Function<? super A, Io<B>> f) {
    // Java17 equivalent (conceptual):
    // return t.fold(
    // a -> f.apply(a).map(Tree::leaf),
    // (li, ri) -> li.flatMap(l -> ri.map(r -> Tree.branch(l, r))));
    return switch (t) {
      case Tree.Leaf(var v) -> f.apply(v).map(Tree::leaf);
      case Tree.Branch(var l, var r) ->
        traverseIo(l, f).flatMap(lb -> traverseIo(r, f).map(rb -> Tree.branch(lb, rb)));
    };
  }

  public static <A> Io<Tree<A>> sequenceIo(Tree<Io<A>> t) {
    return traverseIo(t, Function.identity());
  }

  // ---------------------------------------------------------------------------
  // fp.data.List × Option / Either / Try / Io (uses ADT pattern matching)
  // ---------------------------------------------------------------------------

  // --- Option ---

  public static <A, B> Option<List<B>> traverseOption(
      List<A> as,
      Function<? super A, Option<B>> f) {

    // Java17 equivalent (conceptual, foldRight):
    // return as.foldRight(Option.some(List.empty()),
    // (a, acc) -> acc.flatMap(xs -> f.apply(a).map(b ->
    // List.cons(b, xs)))
    // );

    return switch (as) {
      case List.Nil() -> Option.some(List.empty());
      case List.Cons(var h, var t) ->
        f.apply(h).flatMap(b -> traverseOption(t, f).map(xs -> List.cons(b, xs)));
    };
  }

  public static <A> Option<List<A>> sequenceOption(List<Option<A>> as) {
    return traverseOption(as, Function.identity());
  }

  // --- Either ---

  public static <L, A, B> Either<L, List<B>> traverseEither(
      List<A> as,
      Function<? super A, Either<L, B>> f) {

    // Java17 equivalent (conceptual, foldRight):
    // return as.foldRight(Either.right(List.empty()),
    // (a, acc) -> acc.flatMap(xs -> f.apply(a).map(b ->
    // List.cons(b, xs)))
    // );

    return switch (as) {
      case List.Nil() -> Either.right(List.empty());
      case List.Cons(var h, var t) -> f.apply(h).flatMap(b -> traverseEither(t, f).map(xs -> List.cons(b, xs)));
    };
  }

  public static <L, A> Either<L, List<A>> sequenceEither(List<Either<L, A>> as) {
    return traverseEither(as, Function.identity());
  }

  // --- Try ---

  public static <A, B> Try<List<B>> traverseTry(List<A> as, Function<? super A, Try<B>> f) {

    // Java17 equivalent (conceptual, foldRight):
    // return as.foldRight(Try.success(List.empty()),
    // (a, acc) -> acc.flatMap(xs -> f.apply(a).map(b ->
    // List.cons(b, xs)))
    // );

    return switch (as) {
      case List.Nil() ->
        Try.success(List.empty());
      case List.Cons(var h, var t) ->
        f.apply(h).flatMap(b -> traverseTry(t, f).map(xs -> List.cons(b, xs)));
    };
  }

  public static <A> Try<List<A>> sequenceTry(List<Try<A>> as) {
    return traverseTry(as, Function.identity());
  }

  // --- Io ---

  public static <A, B> Io<org.example.app.fp.data.List<B>> traverseIo(org.example.app.fp.data.List<A> as,
      Function<? super A, Io<B>> f) {

    // Java17 equivalent (conceptual, foldRight):
    // return as.foldRight(Io.of(List::empty),
    // (a, acc) -> acc.flatMap(xs -> f.apply(a).map(b ->
    // List.cons(b, xs)))
    // );

    return switch (as) {
      case List.Nil() -> Io.of(List::empty);
      case List.Cons(var h, var t) -> f.apply(h).flatMap(b -> traverseIo(t, f).map(xs -> List.cons(b, xs)));
    };
  }

  public static <A> Io<List<A>> sequenceIo(List<Io<A>> as) {
    return traverseIo(as, Function.identity());
  }

  // ---------------------------------------------------------------------------
  // java.util.List × Option / Either / Try / Io
  // ※ Replace with your own immutable List implementation's fold / Cons/Nil if
  // needed
  // ---------------------------------------------------------------------------

  // --- Option ---

  public static <A, B> Option<java.util.List<B>> traverseOption(java.util.List<A> as,
      Function<? super A, Option<B>> f) {
    // Java21 note:
    // java.util.List is *not* an algebraic data type (no Nil/Cons structure), so
    // pattern matching
    // cannot be used as in Tree or custom List. The imperative loop below is the
    // natural form.
    Option<java.util.List<B>> acc = Option.some(new ArrayList<>(as.size()));
    for (A a : as) {
      acc = acc.flatMap(list -> f.apply(a).map(b -> {
        list.add(b);
        return list;
      }));
    }
    return acc;
  }

  public static <A> Option<java.util.List<A>> sequenceOption(java.util.List<Option<A>> xs) {
    // Java21 note:
    // sequenceOption simply delegates to traverseOption, and similarly cannot
    // benefit from
    // pattern matching because java.util.List lacks algebraic constructors.
    return traverseOption(xs, Function.identity());
  }

  // --- Either ---

  public static <L, A, B> Either<L, java.util.List<B>> traverseEither(java.util.List<A> as,
      Function<? super A, Either<L, B>> f) {
    java.util.List<B> out = new ArrayList<>(as.size());
    for (A a : as) {
      Either<L, B> eb = f.apply(a);
      if (eb.isLeft())
        return Either.left(((Either.Left<L, B>) eb).getLeft());
      out.add(((Either.Right<L, B>) eb).getRight());
    }
    return Either.right(out);
  }

  public static <L, A> Either<L, java.util.List<A>> sequenceEither(java.util.List<Either<L, A>> xs) {
    return traverseEither(xs, Function.identity());
  }

  // --- Try ---

  public static <A, B> Try<java.util.List<B>> traverseTry(java.util.List<A> as, Function<? super A, Try<B>> f) {
    java.util.List<B> out = new ArrayList<>(as.size());
    for (A a : as) {
      Try<B> tb = f.apply(a);
      if (tb.isFailure())
        return Try.failure(((Try.Failure<B>) tb).getCause());
      out.add(((Try.Success<B>) tb).value());
    }
    return Try.success(out);
  }

  public static <A> Try<java.util.List<A>> sequenceTry(java.util.List<Try<A>> xs) {
    return traverseTry(xs, Function.identity());
  }

  // --- Io ---

  public static <A, B> Io<java.util.List<B>> traverseIo(java.util.List<A> as, Function<? super A, Io<B>> f) {
    // Sequential traversal is sufficient. For parallel, provide a separate
    // Executor-based version.
    Io<java.util.List<B>> acc = Io.of(ArrayList::new);
    for (A a : as) {
      acc = acc.flatMap(list -> f.apply(a).map(b -> {
        list.add(b);
        return list;
      }));
    }
    return acc;
  }

  public static <A> Io<java.util.List<A>> sequenceIo(java.util.List<Io<A>> xs) {
    return traverseIo(xs, Function.identity());
  }
}