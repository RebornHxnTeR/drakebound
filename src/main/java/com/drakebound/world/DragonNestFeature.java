package com.drakebound.world;

import com.drakebound.Drakebound;
import com.drakebound.block.DragonEggBlock;
import com.drakebound.entity.DragonEntity;
import com.drakebound.entity.DragonVariant;
import com.drakebound.registry.ModRegistries;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * Ninho de dragão: plataforma redonda e irregular de terra queimada, ligeiramente côncava,
 * com manchas de barro compactado, terra infértil, terra enraizada e magma, pilares em ruína na borda e um dragão a guardá-la.
 * Todos têm 1 a 2 baús de tesouro; os machos guardam mais minério bruto e as fêmeas guardam
 * ovos (ou uma cria) sobre o magma central.
 */
public class DragonNestFeature extends Feature<NestConfiguration> {
    private static final int CLEAR_HEIGHT = 5;

    public DragonNestFeature(Codec<NestConfiguration> codec) {
        super(codec);
    }

    /** Pilares em ruína e enchimento por baixo da plataforma, por variante. */
    private record Palette(BlockState pillar, BlockState base) {
    }

    private static Palette palette(DragonVariant variant) {
        return switch (variant) {
            case GREEN -> new Palette(Blocks.MOSSY_COBBLESTONE.defaultBlockState(), Blocks.DIRT.defaultBlockState());
            case YELLOW -> new Palette(Blocks.COBBLED_DEEPSLATE.defaultBlockState(), Blocks.STONE.defaultBlockState());
            case RED, GREY -> new Palette(Blocks.BLACKSTONE.defaultBlockState(), Blocks.DIRT.defaultBlockState());
            case NETHER_ELDER -> new Palette(Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState(), Blocks.NETHERRACK.defaultBlockState());
        };
    }

    @Override
    public boolean place(FeaturePlaceContext<NestConfiguration> context) {
        NestConfiguration config = context.config();
        RandomSource random = context.random();
        if (random.nextFloat() >= config.chance()) {
            return false;
        }
        WorldGenLevel level = context.level();
        BlockPos center = context.origin().below();
        DragonVariant variant = config.variant();
        Palette palette = palette(variant);
        boolean elder = variant == DragonVariant.NETHER_ELDER;
        boolean male = random.nextBoolean();
        int radius = elder ? 11 + random.nextInt(3) : 7 + random.nextInt(3);

        BlockState scorched = ModRegistries.SCORCHED_EARTH.get().defaultBlockState();
        BlockState magma = Blocks.MAGMA_BLOCK.defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();
        // Fase aleatória das ondulações da borda: dá a forma irregular sem ruído caro.
        float phaseA = random.nextFloat() * Mth.TWO_PI;
        float phaseB = random.nextFloat() * Mth.TWO_PI;

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int dx = -radius - 1; dx <= radius + 1; dx++) {
            for (int dz = -radius - 1; dz <= radius + 1; dz++) {
                float dist = Mth.sqrt(dx * dx + dz * dz);
                float angle = (float) Mth.atan2(dz, dx);
                float edge = radius + Mth.sin(angle * 3.0F + phaseA) * 1.2F + Mth.sin(angle * 7.0F + phaseB) * 0.6F;
                if (dist > edge) {
                    continue;
                }
                // Tigela: o centro fica um bloco abaixo e a borda um bloco acima.
                float t = dist / edge;
                int y = center.getY() + (t < 0.35F ? -1 : t > 0.85F ? 1 : 0);
                int x = center.getX() + dx;
                int z = center.getZ() + dz;

                for (int dy = 1; dy <= CLEAR_HEIGHT; dy++) {
                    this.setBlock(level, pos.set(x, y + dy, z), air);
                }
                for (int dy = 1; dy <= 3; dy++) {
                    this.setBlock(level, pos.set(x, y - dy, z), palette.base);
                }

                BlockState top = Math.abs(dx) <= 1 && Math.abs(dz) <= 1 ? magma : floorBlock(random.nextFloat(), scorched);
                this.setBlock(level, pos.set(x, y, z), top);

                if (t > 0.35F && t < 0.85F) {
                    this.decorateTreasure(level, pos.set(x, y + 1, z), random, male, elder);
                }
            }
        }

        this.placePillars(level, center, radius, random, palette.pillar);

        for (int i = 0, chests = 1 + random.nextInt(2); i < chests; i++) {
            float angle = random.nextFloat() * Mth.TWO_PI;
            float d = radius * (0.4F + random.nextFloat() * 0.3F);
            pos.set(center.getX() + Mth.floor(Mth.cos(angle) * d), center.getY() + 1, center.getZ() + Mth.floor(Mth.sin(angle) * d));
            this.setBlock(level, pos, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, net.minecraft.core.Direction.Plane.HORIZONTAL.getRandomDirection(random)));
            RandomizableContainer.setBlockEntityLootTable(level, random, pos, treasure(variant));
        }
        if (!male) {
            this.placeEggsOrHatchling(level, center, random, variant);
        }

        this.spawnGuardian(level, center.above(2), random, variant, male, false);
        return true;
    }

    /** Cada variante tem o seu baú, porque a escama no tesouro é da cor do dragão. */
    private static ResourceKey<LootTable> treasure(DragonVariant variant) {
        return ResourceKey.create(Registries.LOOT_TABLE,
            Identifier.fromNamespaceAndPath(Drakebound.MOD_ID, "chests/" + variant.getSerializedName() + "_dragon_nest"));
    }

    /** Chão do ninho: sobretudo terra queimada, com manchas dos restantes blocos pedidos. */
    private static BlockState floorBlock(float roll, BlockState scorched) {
        if (roll < 0.06F) return Blocks.MAGMA_BLOCK.defaultBlockState();
        if (roll < 0.16F) return Blocks.COARSE_DIRT.defaultBlockState();
        if (roll < 0.24F) return Blocks.PACKED_MUD.defaultBlockState();
        if (roll < 0.30F) return Blocks.ROOTED_DIRT.defaultBlockState();
        return scorched;
    }

    /** Minério bruto amontoado; os machos guardam o dobro. */
    private void decorateTreasure(WorldGenLevel level, BlockPos pos, RandomSource random, boolean male, boolean elder) {
        float roll = random.nextFloat() / (male ? 2.0F : 1.0F) / (elder ? 1.5F : 1.0F);
        if (roll < 0.012F) {
            this.setBlock(level, pos, Blocks.RAW_GOLD_BLOCK.defaultBlockState());
        } else if (roll < 0.024F) {
            this.setBlock(level, pos, Blocks.RAW_IRON_BLOCK.defaultBlockState());
        } else if (roll < 0.036F) {
            this.setBlock(level, pos, Blocks.RAW_COPPER_BLOCK.defaultBlockState());
        }
    }

    private void placePillars(WorldGenLevel level, BlockPos center, int radius, RandomSource random, BlockState pillar) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int pillars = 3 + random.nextInt(3);
        float step = Mth.TWO_PI / pillars;
        for (int i = 0; i < pillars; i++) {
            float angle = i * step + random.nextFloat() * step * 0.5F;
            int x = center.getX() + Mth.floor(Mth.cos(angle) * (radius - 1));
            int z = center.getZ() + Mth.floor(Mth.sin(angle) * (radius - 1));
            int height = 2 + random.nextInt(4);
            for (int dy = 2; dy < 2 + height; dy++) {
                this.setBlock(level, pos.set(x, center.getY() + dy, z), pillar);
            }
        }
    }

    /** Fêmeas guardam 1 a 3 ovos sobre o magma central; 30% das vezes já há uma cria. */
    private void placeEggsOrHatchling(WorldGenLevel level, BlockPos center, RandomSource random, DragonVariant variant) {
        if (random.nextFloat() < 0.3F) {
            this.spawnGuardian(level, center, random, variant, random.nextBoolean(), true);
            return;
        }
        DragonEggBlock egg = ModRegistries.EGG_BLOCKS.get(variant).get();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int i = 0, eggs = 1 + random.nextInt(3); i < eggs; i++) {
            pos.set(center.getX() + random.nextInt(3) - 1, center.getY(), center.getZ() + random.nextInt(3) - 1);
            if (level.getBlockState(pos).isAir()) {
                this.setBlock(level, pos, egg.defaultBlockState());
                DragonEggBlock.scheduleCheck(level, pos.immutable(), egg, random);
            }
        }
    }

    private void spawnGuardian(WorldGenLevel level, BlockPos pos, RandomSource random, DragonVariant variant, boolean male, boolean baby) {
        DragonEntity dragon = ModRegistries.DRAGON.get().create(level.getLevel(), EntitySpawnReason.STRUCTURE);
        if (dragon == null) {
            return;
        }
        dragon.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, random.nextFloat() * 360.0F, 0.0F);
        dragon.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), EntitySpawnReason.STRUCTURE, null);
        dragon.setVariant(variant);
        dragon.setMale(male);
        if (baby) {
            dragon.setAge(DragonEntity.BABY_START_AGE);
        }
        dragon.setHomeTo(pos, DragonEntity.NEST_RADIUS);
        dragon.setPersistenceRequired();
        level.addFreshEntityWithPassengers(dragon);
    }
}
