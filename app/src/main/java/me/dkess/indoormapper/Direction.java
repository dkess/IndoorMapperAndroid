package me.dkess.indoormapper;

public enum Direction {
    forward, right, backward, left;

    public Direction add(Direction other) {
        return Direction.values()[(this.ordinal() + other.ordinal()) % 4];
    }

    public Direction sub(Direction other) {
        return Direction.values()[(4 + this.ordinal() - other.ordinal()) % 4];
    }

    public Direction opposite() {
        return this.add(Direction.backward);
    }

    public static Direction fromLetter(char c) {
        return Direction.values()["wsda".indexOf(c)];
    }

}
