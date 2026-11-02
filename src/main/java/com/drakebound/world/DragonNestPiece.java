package com.drakebound.world;

import com.drakebound.Drakebound;
import com.drakebound.block.DragonEggBlock;
import com.drakebound.block.DragonSkullBlock;
import com.drakebound.entity.DragonEntity;
import com.drakebound.entity.DragonVariant;
import com.drakebound.registry.ModRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * O ninho é gerado chunk a chunk (postProcess corre uma vez por chunk que o ninho toca).
 * Para ser igual em todos os chunks: as decisões globais (pilares, baús, ovos) usam um gerador
 * criado sempre com a mesma semente, e as decisões por coluna usam um hash da posição.
 * Cada chamada só percorre as colunas do chunk atual.
 */
public class DragonNestPiece extends StructurePiece {
    /** Espaço livre por cima da plataforma para os dragões se mexerem (também abre a gruta nos ninhos subterrâneos). */
    private static final int CLEAR_HEIGHT = 10;
    /** Quanto o terreno pode subir ou descer em relação ao centro; colunas fora disto ficam como estão. */
    private static final int SCAN = 12;
    private static final float ABANDONED_CHANCE = 0.2F;

    private final DragonVariant variant;
    private final boolean male;
    private final BlockPos center;
    private final int radius;
    private final long seed;

    public DragonNestPiece(DragonVariant variant, boolean male, BlockPos center, int radius, long seed) {
        super(ModRegistries.DRAGON_NEST_PIECE.get(), 0, new BoundingBox(
            center.getX() - radius - 2, center.getY() - SCAN - 1, center.getZ() - radius - 2,
            center.getX() + radius + 2, center.getY() + SCAN + CLEAR_HEIGHT + 2, center.getZ() + radius + 2));
        this.variant = variant;
        this.male = male;
        this.center = center;
        this.radius = radius;
        this.seed = seed;
    }

    public DragonNestPiece(CompoundTag tag) {
        super(ModRegistries.DRAGON_NEST_PIECE.get(), tag);
        this.variant = tag.read("variant", DragonVariant.CODEC).orElse(DragonVariant.RED);
        this.male = tag.getBooleanOr("male", true);
        this.center = tag.read("center", BlockPos.CODEC).orElse(BlockPos.ZERO);
        this.radius = tag.getIntOr("radius", 8);
        this.seed = tag.getLongOr("seed", 0L);
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.store("variant", DragonVariant.CODEC, this.variant);
        tag.putBoolean("male", this.male);
        tag.store("center", BlockPos.CODEC, this.center);
        tag.putInt("radius", this.radius);
        tag.putLong("seed", this.seed);
    }

    /** Pilares em ruína, por variante. */
    private static BlockState pillar(DragonVariant variant) {
        return switch (variant) {
            case GREEN -> Blocks.MOSSY_COBBLESTONE.defaultBlockState();
            case YELLOW -> Blocks.COBBLED_DEEPSLATE.defaultBlockState();
            case RED, GREY -> Blocks.BLACKSTONE.defaultBlockState();
            case NETHER_ELDER -> Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
            case ENDER -> Blocks.OBSIDIAN.defaultBlockState();
        };
    }

    /** Chão verdadeiro: ignora ar, plantas, neve, folhas e os ossos do esqueleto (para empilhar as costelas). */
    private static boolean isGround(BlockState state) {
        return !state.canBeReplaced() && !state.is(ModRegistries.DRAGON_BONE_BLOCK.get()) && !state.is(BlockTags.LEAVES) && !state.is(BlockTags.LOGS) && state.getFluidState().isEmpty();
    }

    /** Altura do chão natural nesta coluna (procura só SCAN blocos acima e abaixo do centro); MIN_VALUE se não houver. */
    private int groundY(WorldGenLevel level, int x, int z, BlockPos.MutableBlockPos pos) {
        boolean openAbove = false;
        for (int y = this.center.getY() + SCAN, min = this.center.getY() - SCAN; y >= min; y--) {
            boolean ground = isGround(level.getBlockState(pos.set(x, y, z)));
            if (ground && openAbove) {
                return y;
            }
            openAbove = !ground;
        }
        return Integer.MIN_VALUE;
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator, RandomSource random,
                            BoundingBox chunkBox, ChunkPos chunkPos, BlockPos referencePos) {
        RandomSource rng = RandomSource.create(this.seed);
        float phaseA = rng.nextFloat() * Mth.TWO_PI;
        float phaseB = rng.nextFloat() * Mth.TWO_PI;
        boolean elder = this.variant.powerMultiplier > 1.0F;
        boolean abandoned = this.isAbandoned();

        this.buildPlatform(level, chunkBox, phaseA, phaseB, elder, abandoned);
        this.placePillars(level, chunkBox, rng, pillar(this.variant));
        this.placeChests(level, chunkBox, rng);
        if (abandoned) {
            // Ninho abandonado: sem dragão nem ovos, só o esqueleto do antigo dono.
            this.placeSkeleton(level, chunkBox);
            return;
        }
        if (!this.male) {
            this.placeEggs(level, chunkBox, rng);
        }
        // Entidades só no chunk do centro, para não duplicarem.
        if (chunkBox.isInside(this.center)) {
            this.spawnDragon(level, this.surface(level, this.center.getX(), this.center.getZ()).above(), rng, this.male);
        }
    }

    /**
     * O ninho assenta no terreno tal como ele é: em cada coluna só troca o bloco do chão pelos blocos do ninho
     * e tira plantas, folhas e troncos por cima (até CLEAR_HEIGHT), sem paredes, alicerces nem escavações.
     */
    private void buildPlatform(WorldGenLevel level, BoundingBox chunkBox, float phaseA, float phaseB, boolean elder, boolean abandoned) {
        BlockState scorched = ModRegistries.SCORCHED_EARTH.get().defaultBlockState();
        BlockState magma = Blocks.MAGMA_BLOCK.defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int minX = Math.max(this.boundingBox.minX(), chunkBox.minX());
        int maxX = Math.min(this.boundingBox.maxX(), chunkBox.maxX());
        int minZ = Math.max(this.boundingBox.minZ(), chunkBox.minZ());
        int maxZ = Math.min(this.boundingBox.maxZ(), chunkBox.maxZ());

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                int dx = x - this.center.getX();
                int dz = z - this.center.getZ();
                float dist = Mth.sqrt(dx * dx + dz * dz);
                float angle = (float) Mth.atan2(dz, dx);
                float edge = this.radius + Mth.sin(angle * 3.0F + phaseA) * 1.2F + Mth.sin(angle * 7.0F + phaseB) * 0.6F;
                if (dist > edge) {
                    continue;
                }
                int y = this.groundY(level, x, z, pos);
                if (y == Integer.MIN_VALUE) {
                    continue;
                }
                for (int dy = 1; dy <= CLEAR_HEIGHT; dy++) {
                    BlockState above = level.getBlockState(pos.set(x, y + dy, z));
                    if (above.isAir()) {
                        break;
                    }
                    if (isGround(above) || !above.getFluidState().isEmpty()) {
                        break;
                    }
                    level.setBlock(pos, air, Block.UPDATE_CLIENTS);
                }
                BlockState top = Math.abs(dx) <= 1 && Math.abs(dz) <= 1 ? magma : floorBlock(this.roll(x, z, 0), scorched);
                level.setBlock(pos.set(x, y, z), top, Block.UPDATE_CLIENTS);
                float t = dist / edge;
                if (!abandoned && t > 0.35F && t < 0.85F) {
                    BlockState treasure = treasureBlock(this.roll(x, z, 1), this.male, elder);
                    if (treasure != null) {
                        level.setBlock(pos.set(x, y + 1, z), treasure, Block.UPDATE_CLIENTS);
                    }
                }
            }
        }
    }

    /** Primeiro bloco livre por cima do chão natural (ou o centro, se a coluna não tiver chão perto). */
    private BlockPos surface(WorldGenLevel level, int x, int z) {
        int y = this.groundY(level, x, z, new BlockPos.MutableBlockPos());
        return new BlockPos(x, y == Integer.MIN_VALUE ? this.center.getY() : y, z);
    }

    /** 20% dos ninhos estão abandonados (nunca o ninho único do End). Fixo pela posição, igual em qualquer chunk. */
    private boolean isAbandoned() {
        return this.variant != DragonVariant.ENDER && this.roll(this.center.getX(), this.center.getZ(), 2) < ABANDONED_CHANCE;
    }

    /**
     * Esqueleto de dragão em blocos de osso de dragão, deitado no chão do ninho: coluna, costelas, asas abertas,
     * patas, cauda e o crânio de dragão. Coordenadas relativas ao centro: s ao longo do corpo, p para o lado, h acima do chão.
     */
    private void placeSkeleton(WorldGenLevel level, BoundingBox chunkBox) {
        boolean alongX = this.roll(this.center.getX(), this.center.getZ(), 3) < 0.5F;
        int sign = this.roll(this.center.getX(), this.center.getZ(), 4) < 0.5F ? 1 : -1;
        Direction.Axis spine = alongX ? Direction.Axis.X : Direction.Axis.Z;
        Direction.Axis side = alongX ? Direction.Axis.Z : Direction.Axis.X;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        // Coluna do crânio à ponta da cauda (a cauda ondula um pouco).
        for (int s = -14; s <= 7; s++) {
            int p = s < -7 ? Math.round(Mth.sin(s * 0.6F) * 1.5F) : 0;
            this.bone(level, chunkBox, pos, alongX, sign, s, p, 1, spine);
        }
        // Costelas: arcos de 3 blocos de cada lado.
        for (int s = -4; s <= 3; s += 2) {
            for (int p = -1; p <= 1; p += 2) {
                this.bone(level, chunkBox, pos, alongX, sign, s, 2 * p, 1, Direction.Axis.Y);
                this.bone(level, chunkBox, pos, alongX, sign, s, 2 * p, 2, Direction.Axis.Y);
                this.bone(level, chunkBox, pos, alongX, sign, s, p, 3, side);
            }
        }
        // Crânio de dragão na ponta do pescoço, com o focinho virado para fora.
        Direction facing = alongX ? (sign > 0 ? Direction.EAST : Direction.WEST) : (sign > 0 ? Direction.SOUTH : Direction.NORTH);
        this.place(level, chunkBox, pos, alongX, sign, 8, 0, 1,
            ModRegistries.DRAGON_SKULL.get().defaultBlockState().setValue(DragonSkullBlock.FACING, facing));
        // Asas abertas para trás, a partir dos ombros.
        for (int k = 1; k <= 7; k++) {
            for (int p = -1; p <= 1; p += 2) {
                this.bone(level, chunkBox, pos, alongX, sign, 2 - k / 2, p * (2 + k), 1, side);
            }
        }
        // Patas.
        for (int s : new int[] {-5, 4}) {
            for (int p = -1; p <= 1; p += 2) {
                this.bone(level, chunkBox, pos, alongX, sign, s, 3 * p, 1, side);
                this.bone(level, chunkBox, pos, alongX, sign, s + 1, 4 * p, 1, spine);
            }
        }
    }

    private void bone(WorldGenLevel level, BoundingBox chunkBox, BlockPos.MutableBlockPos pos, boolean alongX, int sign,
                      int s, int p, int h, Direction.Axis axis) {
        this.place(level, chunkBox, pos, alongX, sign, s, p, h, ModRegistries.DRAGON_BONE_BLOCK.get().defaultBlockState()
            .setValue(RotatedPillarBlock.AXIS, axis));
    }

    private void place(WorldGenLevel level, BoundingBox chunkBox, BlockPos.MutableBlockPos pos, boolean alongX, int sign,
                       int s, int p, int h, BlockState state) {
        int x = this.center.getX() + (alongX ? s * sign : p);
        int z = this.center.getZ() + (alongX ? p : s * sign);
        if (!chunkBox.isInside(x, this.center.getY(), z)) {
            return;
        }
        int ground = this.groundY(level, x, z, pos);
        if (ground == Integer.MIN_VALUE) {
            return;
        }
        level.setBlock(pos.set(x, ground + h, z), state, Block.UPDATE_CLIENTS);
    }

    /** Número entre 0 e 1 fixo para cada coluna e canal, igual em qualquer chunk. */
    private float roll(int x, int z, int channel) {
        long h = Mth.getSeed(x, channel, z) ^ this.seed;
        h ^= h >>> 33;
        h *= 0xff51afd7ed558ccdL;
        h ^= h >>> 33;
        return (h >>> 40) / (float) (1 << 24);
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
    private static BlockState treasureBlock(float roll, boolean male, boolean elder) {
        roll = roll / (male ? 2.0F : 1.0F) / (elder ? 1.5F : 1.0F);
        if (roll < 0.012F) return Blocks.RAW_GOLD_BLOCK.defaultBlockState();
        if (roll < 0.024F) return Blocks.RAW_IRON_BLOCK.defaultBlockState();
        if (roll < 0.036F) return Blocks.RAW_COPPER_BLOCK.defaultBlockState();
        return null;
    }

    private void placePillars(WorldGenLevel level, BoundingBox chunkBox, RandomSource rng, BlockState pillar) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int pillars = 3 + rng.nextInt(3);
        float step = Mth.TWO_PI / pillars;
        for (int i = 0; i < pillars; i++) {
            float angle = i * step + rng.nextFloat() * step * 0.5F;
            int height = 2 + rng.nextInt(4);
            int x = this.center.getX() + Mth.floor(Mth.cos(angle) * (this.radius - 1));
            int z = this.center.getZ() + Mth.floor(Mth.sin(angle) * (this.radius - 1));
            if (!chunkBox.isInside(x, this.center.getY(), z)) {
                continue;
            }
            int base = this.surface(level, x, z).getY();
            for (int dy = 1; dy <= height; dy++) {
                level.setBlock(pos.set(x, base + dy, z), pillar, Block.UPDATE_CLIENTS);
            }
        }
    }

    /** 1 a 2 baús de tesouro por ninho; o baú tem a escama da cor do dragão. */
    private void placeChests(WorldGenLevel level, BoundingBox chunkBox, RandomSource rng) {
        ResourceKey<LootTable> table = ResourceKey.create(Registries.LOOT_TABLE,
            Identifier.fromNamespaceAndPath(Drakebound.MOD_ID, "chests/" + this.variant.getSerializedName() + "_dragon_nest"));
        for (int i = 0, chests = 1 + rng.nextInt(2); i < chests; i++) {
            float angle = rng.nextFloat() * Mth.TWO_PI;
            float d = this.radius * (0.4F + rng.nextFloat() * 0.3F);
            Direction facing = Direction.Plane.HORIZONTAL.getRandomDirection(rng);
            long lootSeed = rng.nextLong();
            int x = this.center.getX() + Mth.floor(Mth.cos(angle) * d);
            int z = this.center.getZ() + Mth.floor(Mth.sin(angle) * d);
            if (chunkBox.isInside(x, this.center.getY(), z)) {
                BlockPos pos = this.surface(level, x, z).above();
                level.setBlock(pos, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, facing), Block.UPDATE_CLIENTS);
                if (level.getBlockEntity(pos) instanceof RandomizableContainer container) {
                    container.setLootTable(table, lootSeed);
                }
            }
        }
    }

    /** Probabilidades de 0 e de até 1 ovo (cumulativas); o resto são 2 ovos. */
    private static final float NO_EGG_CHANCE = 0.37F;
    private static final float ONE_EGG_CHANCE = 0.97F;

    /** Fêmeas guardam 0 a 2 ovos sobre o magma central (37% / 60% / 3%). Não nascem crias no mundo: só de ovos chocados. */
    private void placeEggs(WorldGenLevel level, BoundingBox chunkBox, RandomSource rng) {
        float roll = rng.nextFloat();
        int eggs = roll < NO_EGG_CHANCE ? 0 : roll < ONE_EGG_CHANCE ? 1 : 2;
        if (!chunkBox.isInside(this.center)) {
            return;
        }
        DragonEggBlock egg = ModRegistries.EGG_BLOCKS.get(this.variant).get();
        // Posições fixas e distintas para os dois ovos não caírem no mesmo bloco.
        for (int i = 0; i < eggs; i++) {
            BlockPos pos = this.surface(level, this.center.getX() + i, this.center.getZ()).above();
            if (level.getBlockState(pos).isAir()) {
                level.setBlock(pos, egg.defaultBlockState(), Block.UPDATE_CLIENTS);
            }
        }
    }

    private void spawnDragon(WorldGenLevel level, BlockPos pos, RandomSource rng, boolean male) {
        DragonEntity dragon = ModRegistries.DRAGON.get().create(level.getLevel(), EntitySpawnReason.STRUCTURE);
        if (dragon == null) {
            return;
        }
        dragon.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, rng.nextFloat() * 360.0F, 0.0F);
        dragon.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), EntitySpawnReason.STRUCTURE, null);
        dragon.setVariant(this.variant);
        dragon.setMale(male);
        dragon.setHomeTo(this.center, DragonEntity.NEST_RADIUS);
        dragon.setPersistenceRequired();
        level.addFreshEntityWithPassengers(dragon);
    }
}
