package com.drakebound.entity;

/**
 * Fases de crescimento. A idade (ticks) vem do AgeableMob: começa em -72000 (60 min) e sobe até 0.
 * Cada fase tem o seu tamanho; o recém-nascido é um pouco mais pequeno do que o bebé.
 */
public enum GrowthStage {
    NEWBORN(0.2F, 0.1F),
    BABY(0.3F, 0.2F),
    CHILD(0.45F, 0.35F),
    TEEN(0.7F, 0.6F),
    ADULT(1.0F, 1.0F);

    /** Ticks que ainda faltam para adulto no início de cada fase (90%, 70% e 45% do total). */
    private static final int BABY_FROM = -DragonEntity.BABY_START_AGE * 9 / 10;
    private static final int CHILD_FROM = -DragonEntity.BABY_START_AGE * 7 / 10;
    private static final int TEEN_FROM = -DragonEntity.BABY_START_AGE * 45 / 100;

    private static final GrowthStage[] VALUES = values();
    public final float scale;
    public final float healthFactor;

    GrowthStage(float scale, float healthFactor) {
        this.scale = scale;
        this.healthFactor = healthFactor;
    }

    public static GrowthStage byId(int ordinal) {
        return ordinal >= 0 && ordinal < VALUES.length ? VALUES[ordinal] : ADULT;
    }

    public static GrowthStage fromAge(int age) {
        int left = -age;
        return left <= 0 ? ADULT : left <= TEEN_FROM ? TEEN : left <= CHILD_FROM ? CHILD : left <= BABY_FROM ? BABY : NEWBORN;
    }
}
