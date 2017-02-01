package me.dkess.indoormapper;

import java.util.Objects;

/**
 * A sorted pair of integers
 */
public class IntPair {
    public final int a, b;

    public IntPair(int a, int b) {
        this.a = a;
        this.b = b;
    }

    public IntPair reversed() {
        return new IntPair(b, a);
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        IntPair p = (IntPair) o;

        return a == p.a && b == p.b;
    }

    public int hashCode() {
        return 997 * a + b;
    }
}
