package com.drakebound.entity;

import com.mojang.serialization.Codec;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import org.jspecify.annotations.Nullable;

/**
 * Tabela de dados de cada variante. Todo o comportamento específico (sopro, atributos, cores)
 * vem daqui, para que a DragonEntity não precise de ramificações por variante.
 */
public enum DragonVariant implements StringRepresentable {
    //        id              dark      light     eyes      mult  particle                      effect               amp  fireSec
    GREEN("green",          0x1E3A12, 0x7BC74D, 0xE8F25A, 1.0F, ParticleTypes.SNEEZE,           MobEffects.POISON,    1,   0),
    YELLOW("yellow",        0x5A4A0C, 0xF2D23C, 0x7FE8FF, 1.0F, ParticleTypes.ELECTRIC_SPARK,   MobEffects.SLOWNESS,  2,   0),
    RED("red",              0x4A0E0E, 0xD8442E, 0xFFC23A, 1.0F,  ParticleTypes.FLAME,            null,                 0,   5),
    GREY("grey",            0x2A2A2E, 0x9A9AA4, 0xB040FF, 1.0F, ParticleTypes.SQUID_INK,        MobEffects.DARKNESS,  0,   0),
    NETHER_ELDER("nether_elder", 0x2B0A05, 0xFF8A1E, 0xFFF4B0, 1.5F, ParticleTypes.SOUL_FIRE_FLAME, MobEffects.WITHER,  1,   6);

    public static final Codec<DragonVariant> CODEC = StringRepresentable.fromEnum(DragonVariant::values);
    private static final DragonVariant[] VALUES = values();

    private final String id;
    /** Cor das escamas escuras e claras usada para recolorir a textura base do dragão. */
    public final int darkColor, lightColor, eyeColor;
    /** Multiplicador de vida e dano (o Ancião do Nether é mais forte). */
    public final float powerMultiplier;
    public final SimpleParticleType breathParticle;
    public final @Nullable Holder<MobEffect> breathEffect;
    public final int breathAmplifier;
    public final int breathFireSeconds;

    DragonVariant(String id, int darkColor, int lightColor, int eyeColor, float powerMultiplier,
                  SimpleParticleType breathParticle, @Nullable Holder<MobEffect> breathEffect, int breathAmplifier, int breathFireSeconds) {
        this.id = id;
        this.darkColor = darkColor;
        this.lightColor = lightColor;
        this.eyeColor = eyeColor;
        this.powerMultiplier = powerMultiplier;
        this.breathParticle = breathParticle;
        this.breathEffect = breathEffect;
        this.breathAmplifier = breathAmplifier;
        this.breathFireSeconds = breathFireSeconds;
    }

    public static DragonVariant byId(int ordinal) {
        return ordinal >= 0 && ordinal < VALUES.length ? VALUES[ordinal] : GREEN;
    }

    @Override
    public String getSerializedName() {
        return this.id;
    }
}
