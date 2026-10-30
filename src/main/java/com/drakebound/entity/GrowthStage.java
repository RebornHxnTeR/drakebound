package com.drakebound.entity;

/** Fases de crescimento. A idade (ticks) vem do AgeableMob: começa em -72000 e sobe até 0. */
public enum GrowthStage {
    HATCHLING(0.3F, 0.15F),
    JUVENILE(0.6F, 0.5F),
    ADULT(1.0F, 1.0F);

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
        return age >= 0 ? ADULT : age >= DragonEntity.BABY_START_AGE / 3 * 2 ? JUVENILE : HATCHLING;
    }
}
