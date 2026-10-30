package com.drakebound.world;

import com.drakebound.entity.DragonVariant;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

/**
 * @param variant variante do dragão que guarda o ninho
 * @param chance  probabilidade de o ninho aparecer quando a colocação (rarity_filter) é escolhida
 * @param fixedPosition se definido, o ninho é único e só aparece nesta posição (X/Z; o Y vem do terreno)
 */
public record NestConfiguration(DragonVariant variant, float chance, Optional<BlockPos> fixedPosition) implements FeatureConfiguration {
    public static final Codec<NestConfiguration> CODEC = RecordCodecBuilder.create(i -> i.group(
        DragonVariant.CODEC.fieldOf("variant").forGetter(NestConfiguration::variant),
        Codec.floatRange(0.0F, 1.0F).fieldOf("chance").forGetter(NestConfiguration::chance),
        BlockPos.CODEC.optionalFieldOf("fixed_position").forGetter(NestConfiguration::fixedPosition)
    ).apply(i, NestConfiguration::new));
}
