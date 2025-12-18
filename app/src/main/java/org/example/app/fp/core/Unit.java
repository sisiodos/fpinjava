package org.example.app.fp.core;

/**
 * Represents a value with no computational content.
 * Equivalent to {@code void} in imperative code, but as a first-class type.
 * Used in functional programming to indicate the presence of a side effect
 * or an operation that returns no meaningful value.
 */
public enum Unit {
  INSTANCE;
}
/**
 * Unit is a single-value type.
 *
 * Java21 pattern matching usage:
 * switch(u) {
 *   case Unit.INSTANCE -> ...
 * }
 */