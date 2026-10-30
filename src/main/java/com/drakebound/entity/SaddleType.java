package com.drakebound.entity;

/** Sela equipada no dragão; o número de lugares define quantos jogadores pode transportar. */
public enum SaddleType {
    NONE(0),
    SINGLE(1),
    DOUBLE(2);

    private static final SaddleType[] VALUES = values();
    public final int seats;

    SaddleType(int seats) {
        this.seats = seats;
    }

    public static SaddleType byId(int ordinal) {
        return ordinal >= 0 && ordinal < VALUES.length ? VALUES[ordinal] : NONE;
    }
}
