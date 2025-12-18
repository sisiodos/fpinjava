package org.example.app.fp.data;

import java.util.Comparator;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * A purely functional binary tree with two cases: {@link Leaf} and
 * {@link Branch}.
 * <p>
 * This is an algebraic data type (ADT) encoded with Java's
 * {@code sealed interface} and {@code record}.
 * Most operations are defined via {@link #fold(Function, BiFunction)} (the
 * catamorphism).
 *
 * <pre>{@code
 * Tree<Integer> t = Tree.branch(
 *     Tree.branch(Tree.leaf(1), Tree.leaf(2)),
 *     Tree.leaf(3));
 *
 * int size = t.size(); // 5 (3 leaves + 2 branches)
 * int depth = t.depth(); // 2
 * int max = t.maximum(Comparator.naturalOrder()); // 3
 * Tree<Integer> doubled = t.map(x -> x * 2);
 * }</pre>
 */
public sealed interface Tree<A> permits Tree.Leaf, Tree.Branch {

  // -------------------------------------------------------------------------
  // Core: fold
  // -------------------------------------------------------------------------

  /**
   * Deconstructs the tree and folds it into a single value.
   *
   * @param onLeaf   handler to turn a leaf value into the accumulator type
   * @param onBranch combiner to merge the two accumulated results (left, right)
   * @param <B>      result type
   */
  <B> B fold(
      Function<? super A, ? extends B> onLeaf,
      BiFunction<? super B, ? super B, ? extends B> onBranch);

  // -------------------------------------------------------------------------
  // Constructors
  // -------------------------------------------------------------------------

  /** Create a leaf. */
  static <A> Tree<A> leaf(A a) {
    return new Leaf<>(a);
  }

  /** Create a branch from two subtrees. */
  static <A> Tree<A> branch(Tree<A> left, Tree<A> right) {
    return new Branch<>(left, right);
  }

  // -------------------------------------------------------------------------
  // Functor / Monad-ish ops
  // -------------------------------------------------------------------------

  /**
   * Maps the value(s) in this tree, preserving shape.
   */
  default <B> Tree<B> map(Function<? super A, ? extends B> f) {
    // Java17 equivalent:
    // return this.<Tree<B>>fold(
    //   a -> Tree.leaf(f.apply(a)),
    //   Tree::branch
    // );
    return switch (this) {
      case Leaf(var v) -> Tree.leaf(f.apply(v));
      case Branch(var l, var r) -> Tree.branch(l.map(f), r.map(f));
    };
  }

  /**
   * Flat-maps values to subtrees and concatenates them by branching.
   * (This is one lawful encoding of a "bind"-like operation for this Tree shape.)
   */
  default <B> Tree<B> flatMap(Function<? super A, ? extends Tree<B>> f) {
    // Java17 equivalent:
    // return this.<Tree<B>>fold(
    //   f,
    //   Tree::branch
    // );
    return switch (this) {
      case Leaf(var v) -> f.apply(v);
      case Branch(var l, var r) -> Tree.branch(l.flatMap(f), r.flatMap(f));
    };
  }

  // -------------------------------------------------------------------------
  // Structural queries
  // -------------------------------------------------------------------------

  /**
   * Number of nodes (both leaves and branches).
   */
  default int size() {
    // Java17 equivalent:
    // return fold(
    //   a -> 1,
    //   (l, r) -> 1 + l + r
    // );
    return switch (this) {
      case Leaf(var v) -> 1;
      case Branch(var l, var r) -> 1 + l.size() + r.size();
    };
  }

  /**
   * Depth (height) of the tree: max number of edges from root to any leaf.
   * Leaves have depth 0, a branch with two leaves has depth 1, etc.
   */
  default int depth() {
    // Java17 equivalent:
    // return fold(
    //   a -> 0,
    //   (l, r) -> 1 + Math.max(l, r)
    // );
    return switch (this) {
      case Leaf(var v) -> 0;
      case Branch(var l, var r) -> 1 + Math.max(l.depth(), r.depth());
    };
  }

  /**
   * Computes the maximum value under a given comparator.
   * For natural order you can pass {@code Comparator.naturalOrder()}.
   */
  default A maximum(Comparator<? super A> cmp) {
    // Java17 equivalent:
    // return fold(
    //   a -> a,
    //   (l, r) -> cmp.compare(l, r) >= 0 ? l : r
    // );
    return switch (this) {
      case Leaf(var v) -> v;
      case Branch(var l, var r) -> {
        A ml = l.maximum(cmp);
        A mr = r.maximum(cmp);
        yield cmp.compare(ml, mr) >= 0 ? ml : mr;
      }
    };
  }

  /**
   * General aggregation over the tree values using a monoid-like combiner.
   * Example: sum with {@code foldMap(a -> a, Integer::sum)}.
   */
  default <B> B foldMap(Function<? super A, ? extends B> to,
      BiFunction<? super B, ? super B, ? extends B> combine) {
    // Java17 equivalent:
    // return fold(to, combine);
    return switch (this) {
      case Leaf(var v) -> to.apply(v);
      case Branch(var l, var r) -> {
        B bl = l.foldMap(to, combine);
        B br = r.foldMap(to, combine);
        yield combine.apply(bl, br);
      }
    };
  }

  // -------------------------------------------------------------------------
  // Cases
  // -------------------------------------------------------------------------

  /**
   * Leaf node holding a value.
   */
  record Leaf<A>(A value) implements Tree<A> {
    @Override
    public <B> B fold(Function<? super A, ? extends B> onLeaf,
        BiFunction<? super B, ? super B, ? extends B> onBranch) {
      // Java21 equivalent (conceptual):
      // return switch (this) {
      //   case Leaf(var v) -> onLeaf.apply(v);
      //   case Branch(var l, var r) -> throw new IllegalStateException("Impossible");
      // };
      return onLeaf.apply(value);
    }

    @Override
    public String toString() {
      return "Leaf(" + value + ")";
    }
  }

  /**
   * Branch node with two subtrees.
   */
  record Branch<A>(Tree<A> left, Tree<A> right) implements Tree<A> {
    @Override
    public <B> B fold(Function<? super A, ? extends B> onLeaf,
        BiFunction<? super B, ? super B, ? extends B> onBranch) {
      // Java21 equivalent (conceptual):
      // return switch (this) {
      //   case Leaf(var v) -> onLeaf.apply(v);
      //   case Branch(var l, var r) -> {
      //       B bl = l.fold(onLeaf, onBranch);
      //       B br = r.fold(onLeaf, onBranch);
      //       yield onBranch.apply(bl, br);
      //   }
      // };
      B l = left.fold(onLeaf, onBranch);
      B r = right.fold(onLeaf, onBranch);
      return onBranch.apply(l, r);
    }

    @Override
    public String toString() {
      return "Branch(" + left + ", " + right + ")";
    }
  }
}