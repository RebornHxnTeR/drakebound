package com.drakebound.world;

import com.drakebound.entity.DragonVariant;
import com.drakebound.registry.ModRegistries;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

/**
 * Ninho de dragão como estrutura, para poder ser encontrado com {@code /locate structure drakebound:<variante>_dragon_nest}.
 * A frequência vem do structure_set (spacing/separation) multiplicada por {@code chance}.
 */
public class DragonNestStructure extends Structure {
    public static final MapCodec<DragonNestStructure> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
        settingsCodec(i),
        DragonVariant.CODEC.fieldOf("variant").forGetter(s -> s.variant),
        Codec.floatRange(0.0F, 1.0F).fieldOf("chance").forGetter(s -> s.chance),
        Placement.CODEC.fieldOf("placement").forGetter(s -> s.placement),
        Codec.INT.optionalFieldOf("min_y", -64).forGetter(s -> s.minY),
        Codec.INT.optionalFieldOf("max_y", 320).forGetter(s -> s.maxY),
        BlockPos.CODEC.optionalFieldOf("fixed_position").forGetter(s -> s.fixedPosition)
    ).apply(i, DragonNestStructure::new));

    /** SURFACE: à superfície do terreno. FLOOR: no chão de uma gruta entre min_y e max_y (Cavernas Verdejantes, Nether). */
    public enum Placement implements StringRepresentable {
        SURFACE, FLOOR;

        public static final Codec<Placement> CODEC = StringRepresentable.fromEnum(Placement::values);

        @Override
        public String getSerializedName() {
            return this == SURFACE ? "surface" : "floor";
        }
    }

    private final DragonVariant variant;
    private final float chance;
    private final Placement placement;
    private final int minY;
    private final int maxY;
    private final Optional<BlockPos> fixedPosition;

    public DragonNestStructure(StructureSettings settings, DragonVariant variant, float chance, Placement placement, int minY, int maxY,
                               Optional<BlockPos> fixedPosition) {
        super(settings);
        this.variant = variant;
        this.chance = chance;
        this.placement = placement;
        this.minY = minY;
        this.maxY = maxY;
        this.fixedPosition = fixedPosition;
    }

    @Override
    public Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        WorldgenRandom random = context.random();
        ChunkPos chunk = context.chunkPos();
        int x;
        int z;
        if (this.fixedPosition.isPresent()) {
            // Ninho único (Dragão do End): só o chunk que contém a posição fixa o gera.
            BlockPos fixed = this.fixedPosition.get();
            if (chunk.x() != (fixed.getX() >> 4) || chunk.z() != (fixed.getZ() >> 4)) {
                return Optional.empty();
            }
            x = fixed.getX();
            z = fixed.getZ();
        } else {
            if (random.nextFloat() >= this.chance) {
                return Optional.empty();
            }
            x = chunk.getMinBlockX() + random.nextInt(16);
            z = chunk.getMinBlockZ() + random.nextInt(16);
        }

        int y = this.placement == Placement.SURFACE
            ? context.chunkGenerator().getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState()) - 1
            : this.findFloor(context, x, z);
        if (y <= context.heightAccessor().getMinY() + 4) {
            return Optional.empty();
        }

        BlockPos center = new BlockPos(x, y, z);
        boolean male = random.nextBoolean();
        // Ninhos à medida de um dragão do tamanho do Ender Dragon.
        int radius = this.variant.powerMultiplier > 1.2F ? 16 + random.nextInt(3) : 12 + random.nextInt(3);
        long seed = random.nextLong();
        return Optional.of(new GenerationStub(center, builder -> builder.addPiece(new DragonNestPiece(this.variant, male, center, radius, seed))));
    }

    /** Desce pela coluna de ruído até encontrar ar sobre um bloco sólido. Devolve minY-1 se não houver. */
    private int findFloor(GenerationContext context, int x, int z) {
        NoiseColumn column = context.chunkGenerator().getBaseColumn(x, z, context.heightAccessor(), context.randomState());
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, 0, z);
        int min = Math.max(this.minY, context.heightAccessor().getMinY() + 1);
        for (int y = this.maxY; y > min; y--) {
            if (column.getBlock(y).isAir()) {
                BlockState below = column.getBlock(y - 1);
                if (below.isFaceSturdy(EmptyBlockGetter.INSTANCE, pos.setY(y - 1), Direction.UP)) {
                    return y - 1;
                }
            }
        }
        return context.heightAccessor().getMinY();
    }

    @Override
    public StructureType<?> type() {
        return ModRegistries.DRAGON_NEST_STRUCTURE.get();
    }
}
