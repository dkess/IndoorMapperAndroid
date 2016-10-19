package me.dkess.indoormapper;

import java.io.Serializable;

/**
 * Created by daniel on 10/18/16.
 */

public class IntString implements Serializable {
    final int n;
    final String s;

    public IntString(int n, String s) {
        this.n = n;
        this.s = s;
    }
}
