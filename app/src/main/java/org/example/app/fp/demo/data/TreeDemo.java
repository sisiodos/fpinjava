package org.example.app.fp.demo.data;

import org.example.app.fp.data.Tree;

/**
 * Demonstrates usage of the functional binary Tree structure.
 * Includes map, fold, size, depth, and structural recursion.
 */
public final class TreeDemo {

  public static void main(String[] args) {

    // ------------------------------------------------------------------
    // Tree construction
    //
    // 5
    // / \
    // 3 8
    // / \ / \
    // 1 4 7 9
    // ------------------------------------------------------------------
    @SuppressWarnings("unused")
    Tree<Integer> t = Tree.branch(
        Tree.branch(Tree.leaf(1), Tree.leaf(4)),
        Tree.branch(Tree.leaf(7), Tree.leaf(9))).map(x -> x + 2); // add root later

    // Real tree:
    Tree<Integer> root = Tree.branch(
        Tree.branch(Tree.leaf(1), Tree.leaf(4)),
        Tree.branch(Tree.leaf(7), Tree.leaf(9)));

    Tree<Integer> full = root.map(x -> x + 2); // shift values upward

    System.out.println("Tree: " + full);

    // ------------------------------------------------------------------
    // map
    // ------------------------------------------------------------------
    var doubled = full.map(x -> x * 2);
    System.out.println("map (*2): " + doubled);

    // ------------------------------------------------------------------
    // fold
    // ------------------------------------------------------------------
    int sum = full.fold(
        a -> a,
        (l, r) -> l + r);
    System.out.println("sum via fold: " + sum);

    // ------------------------------------------------------------------
    // size, depth
    // ------------------------------------------------------------------
    System.out.println("size:  " + full.size());
    System.out.println("depth: " + full.depth());

    // ------------------------------------------------------------------
    // count leaves
    // ------------------------------------------------------------------
    int leaves = full.fold(
        a -> 1,
        (l, r) -> l + r);
    System.out.println("leaf count: " + leaves);

    // ------------------------------------------------------------------
    // Searching the tree with recursion +fold
    // ------------------------------------------------------------------
    boolean hasValue7 = full.fold(
        a -> a == 7,
        (l, r) -> l || r);
    System.out.println("contains 7? " + hasValue7);
  }
}