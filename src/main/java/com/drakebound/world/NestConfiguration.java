package com.drakebound.world;

import com.drakebound.entity.DragonVariant;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

/**
 * @param variant variante do dragão que guarda o ninho
 * @param chance  probabilidade de o ninho aparecer quando a colocação (rarity_filter) é escolhida
 */
public record NestConfiguration(DragonVariant variant, float chance) implements FeatureConfiguration {
    public static final Codec<NestConfiguration> CODEC = RecordCodecBuilder.create(i -> i.group(
        DragonVariant.CODEC.fieldOf("variant").forGetter(NestConfiguration::variant),
        Codec.floatRange(0.0F, 1.0F).fieldOf("chance").forGetter(NestConfiguration::chance)
    ).apply(i, NestConfiguration::new));
}
